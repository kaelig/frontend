# Repressing fronts

## Tests
Tests are failing because of mis pressed data committed
* delete database/pressedPage/*
* check your frontend.properties facia stage is DEV (*not* prod!)
* check you are using your own switches file not the prod or code one
* run admin locally
* turn on facia-press-on-demand switch
* run facia-press locally
* go to http://localhost:9000 just to get the switches file warmed up
* curl -v -X POST http://localhost:9000/press/live/music
* stop facia press and rerun the tests

## shipping
This is more tricky especially if you are making a breaking change (i.e. new facia can't work with the old format and old facia can't work with the new)
### Old facia can use new format, new facia can use the old: Y, Y
Nothing to worry about, just get shipping!
### Old facia can use new format, new facia can use the old: Y, N
If you just ship new facia without pressing things will break.
* turn off auto deploy of preview/training
* goo deploy --block
* merge to master
* when it's on CODE, turn on the facia-press-on-demand switch on CODE
* post to TODO to repress all the code fronts
When you are happy with everything on code:
* deploy facia-press to PROD
* wait a short time (TODO or force it?) for all the fronts to get the new format
* deploy training/preview and check they still work
* deploy facia etc
* have a beer
### Old facia can use new format, new facia can use the old: N, N
You're a bit stuck, maybe find another way to do it!