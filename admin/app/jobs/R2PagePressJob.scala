package jobs

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import common._
import conf.Configuration
import conf.switches.Switches.R2PagePressServiceSwitch
import org.jsoup.Jsoup
import play.api.libs.json.Json
import play.api.libs.ws.WS
import services.R2Archive
import pagepresser.HtmlCleaner._
import play.api.Play.current

object R2PagePressJob extends ExecutionContexts with Logging {
  val waitTimeSeconds = 20

  val queue: TextMessageQueue[SNSNotification] = (Configuration.r2Press.sqsQueueUrl map { queueUrl =>
    val credentials = Configuration.aws.mandatoryCredentials

    TextMessageQueue[SNSNotification](
      new AmazonSQSAsyncClient(credentials).withRegion(Region.getRegion(Regions.EU_WEST_1)),
      queueUrl
    )
  }) getOrElse {
    throw new RuntimeException("Required property 'r2Press.sqsQueueUrl' not set")
  }

  def run() {
    if (R2PagePressServiceSwitch.isSwitchedOn) {
      log.info("R2PagePressJob starting")
      try {
        queue.receive(new ReceiveMessageRequest().withWaitTimeSeconds(waitTimeSeconds)) map ( _ foreach press )
      } catch {
        case e: Exception => log.error(s"### Failed to decode r2 url: ${e.getMessage}", e)
      }
    } else {
      log.info("R2PagePressJob is switched OFF")
    }
  }

  private def press(message: Message[String]) {
    val urlIn = (Json.parse(message.get) \ "Message").as[String]
    if (urlIn.nonEmpty) {
      val url = urlIn.replace("https://", "").replace("http://","")
      WS.url(urlIn).get().map { response =>
        response.status match {
          case 200 => {
            val cleanedHtmlString = clean(Jsoup.parse(response.body)).toString
            R2Archive.putPublic(url, cleanedHtmlString, "text/html")
            R2Archive.get(url).foreach { result =>
              if (result == cleanedHtmlString) {
                // TODO: update DynamoDB
                log.info(s"Pressed $urlIn as $url BUT DON'T FORGET TO UPDATE DYNAMODB!")
              } else {
                log.error(s"Pressed data did not match original for $url")
              }
            }
          }
          case non200 => {
            log.error(s"Unexpected response from $urlIn, status code: $non200")
          }
        }
      }
    } else {
      log.error(s"### invalid url: $urlIn")
    }
    // TODO: only delete if everything is ok?
    queue.delete(message.handle)
  }

}