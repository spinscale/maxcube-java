# maxcube-java - Access max eq3 data via Java and CLI

## TLDR

eq3 is a CLI tool to control your maxcube. You can boost, set devices in holiday mode or get current stats.
While the eq3 has a web based interface I was more interested to control it from my command line. Also learning and 
implementing the data format was fun.

eq3 is licensed under Apache 2 License.

## Installation

Before installing eq3, you should install java8, which is the only dependency. Now you can download the tar.gz archive from the releases page, and run

``` 
tar zxvf eq3-0.0.1.tar.gz
./cli-0.0.1/bin/eq3 version
./cli-0.0.1/bin/eq3
``` 

You should see the downloaded version and a help page.

## CLI - Usage

* `eq3 discover` - Auto discovers a cube in your current network specified by interface
* `eq3 info <ip>` - An alias for `eq3 report cli`
* `eq3 boost  <ip> --room Arbeitszimmer`
* `eq3 holiday  <ip> --room Arbeitszimmer --temperature 25 --end 2h`

If you do not want to specify an ip address or a hostname as second argument all the time (which is likely because
you only own a single cube usually), you can export the environment variable `EQ3_HOST` to the address of your cube
and do not need to specify an IP for the `info`, `boost` or `holiday` commands.


### CLI - Discovery

```bash
# on mac
eq3 discover en0
# on linux
eq3 discover eth0
```

Output

```bash
KEQ0532145   192.168.1.1
```


### CLI - Info

```bash
eq3 info 192.168.1.1
# same as
eq3 report cli 192.168.1.1

╔════════════╤══════════════════╤══════════╗
║ id         │ date             │ firmware ║
╠════════════╪══════════════════╪══════════╣
║ KEQ0537741 │ 2017-01-29T17:36 │ 1.1.3    ║
╚════════════╧══════════════════╧══════════╝

╔════╤═══════════════╤══════╤═════════════╤═════════╤═════════════╤══════╗
║ Id │ Room          │ Temp │ Window open │ Valve % │ Low battery │ Mode ║
╠════╪═══════════════╪══════╪═════════════╪═════════╪═════════════╪══════╣
║ 1  │ Wohnzimmer    │ 23.3 │ false       │ 16      │ false       │ AUTO ║
╟────┼───────────────┼──────┼─────────────┼─────────┼─────────────┼──────╢
║ 2  │ Schlafzimmer  │ 16.9 │ false       │ 0       │ false       │ AUTO ║
╟────┼───────────────┼──────┼─────────────┼─────────┼─────────────┼──────╢
║ 3  │ Bad           │ 18.2 │ false       │ 13      │ false       │ AUTO ║
╟────┼───────────────┼──────┼─────────────┼─────────┼─────────────┼──────╢
║ 4  │ Arbeitszimmer │ 0    │ false       │ 0       │ false       │ AUTO ║
╟────┼───────────────┼──────┼─────────────┼─────────┼─────────────┼──────╢
║ 5  │ Küche         │ 20.9 │ false       │ 0       │ false       │ AUTO ║
╚════╧═══════════════╧══════╧═════════════╧═════════╧═════════════╧══════╝
```



### CLI - Boosting

Boosting allows to open the heating valve for a certain amount of time (this amount of time needs to be configured separately)

```bash
# starts the boost
eq3 boost 192.168.1.1 --room Bedroom
```


### CLI - Holiday mode

```bash
# Sets holiday for a room, allows to change heating for a certain amount of time
eq3 holiday 192.168.1.1 --room Bedroom --duration 60m --temperature 20

# duration can be s (seconds), m (minutes), h (hours) or d (days)
# temperature is always celsius
```

## Reporting issues

If you are reporting an issue, it would be great if you could try to recreate it using the `-d` command, as this enables
debug mode and logs a ton of additional log lines that should be included. Note that this might reveal information about
your setup when you create an issue, so please be comfortable with that.


## Under the hood - all Java

The CLI tool uses a java library itself, so it might make most sense to check out the source in the CLI tool. 
The java library consists of a client that polls the cube. The client is based on Apache MINA. If you wanted,
implementing a web app on top of this would be pretty simple.

* Apache MINA for networking communication, and generic socket communication
* stork for packaging to create a CLI tool, so users dont have to care for java
* airlift for commandline parsing
* logback for logging


## Issues

* Far too much functionality is not exposed yet. Bring your ideas and we can work on them. 
* Also the code was being hacked over the weekend, so dont expect glorious things here
* Test coverage is okay'ish
* Design is pretty bad... remember, weekend hack
* The resulting binary is pretty big with all the dependencies


## Next steps

* Allow to read the IP addr of your cube from a default configuration file, so it does not need to be specified
* CSV renderer, only for the rooms (name, id, temp, valve open in percent)
* add `pair` command
* Read duty cycle and interpret in CLI renderer
* ES renderer
* Add javadocs
* Report/Show weekly program
* Log boost duration per room
* Implement more commands: https://github.com/Bouni/max-cube-protocol
* Maybe rewrite in crystal lang to have a small lean binary and to learn the language


## Further reading decoding the protocol

There are a ton of other implementations, which might be more bugfree than this one, so feel free to check them out

* https://github.com/Bouni/max-cube-protocol
* http://www.domoticaforum.eu/viewtopic.php?f=66&t=6654
* https://github.com/aleszoulek/maxcube/
* https://github.com/ivesdebruycker/maxcube
* https://github.com/ivesdebruycker/maxcube-cli


## Pull request

Please do refrain from submitting pull requests, if you have decompiled the original application from the vendor, as it
is java itself. This library should remain free software. Thank you!


## Debugging on the command line

If you want to extract the base64 that is printed out with the `-d`, you can use tools like `od`

```bash
echo "AARAAAAAB5EAAWY=" | base64 -D | od -A d -t xC
0000000    00  04  40  00  00  00  07  91  00  01  66
0000011
```
