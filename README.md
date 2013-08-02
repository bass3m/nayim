# nayim

This is meant to be a utility that is run periodically in order to update a couch database containing tweets. I use twitter lists to divide my twitter timeline into interests like clojure, sports, friends etc.. The db stores those tweets so i can catch up with my twitter timeline when time permits.
The utility is meant to be run together with a web front-end [baseet](https://github.com/bass3m/baseet). 

This app runs hourly and updates one of my twitter lists in a round-robin fashion (i use a very rudimentary mod hour of day based algorithm).
 
## Usage

`lein run` should all that is required to run.

#### Todo
 - Create a library to minimize duplication of code.
 - Perhaps a more sophisticated tweet update scheduling, not many tweets (at least in my timeline) occurring at 3am.

## License

Copyright © 2013 Bassem Youssef

Distributed under the Eclipse Public License, the same as Clojure.
