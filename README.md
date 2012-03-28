# easyconf

a very easy config tool.

2012-3-28 Release 0.1.0

## Usage

add to porject.clj

```clojure
[easyconf "0.1.0"]
```

### define config items that can be configured at runtime
under easyconf.core namespace, we supply two macro defconf and
defnconf,  the usage of these two macro just like def and defn, but
add the ability to be configured at runtime.

examples

```clojure
(defconf ^{:validator string? :validator-msg "configure value must be
string" :conf-name "conf-string" } conf-item "default value"}

(defnconf game-name [game] (str "game name : " game))
```

the meat key :validator :validator-msg :conf-name  in the define is
optional.

#### options key
     * :validator  => the value must be a function. the easyconf will
     invoke (f value) to identity whether the value is acceptable.
     * :validator-msg => when the value is not invalid ,then easy will
     throw exception with this message.
     * :conf-name => you can chood a different configure name to be
     used other than the default. (must be a keyword)

when you like to make a config var, please use defconf or defnconf, instead of def or defn, like below:
       (defconf max-coin-per-day 5000)
after this, you can use it just as 
      (def max-coin-per-day 5000)
there is no difference if you do nothing else.


### change the value of the configureable items.
under easyconf.core, we also supply two function config and
config-once to change the value of the configureable items.

the only difference of these two functions is when you invoke
conf-once two times with same conf-name , easyconf will throw a
exception that does not allow you to do it, and conf does not supply
such examination.

```clojure
(config :conf-string "changed config value")
(config-once :conf-string "changed config value")

;;the code below will trigger a exception
(config-once :conf-string "change config value twice")  
```

## write autoload configure file.
if you put a file under config namespace, the file will be
automaticly loaded when you first using easyconf.

### recommondation

you can put all you config value in a separator folder, and put the
folder into classpath at the production phase.

example

```clojure
(ns config.autoload
  (:use [easyconf.core]))

(config-once :conf-1 "conf-1")
(config-once :conf-two "conf-two")
(config-once :game-name (fn [id] (get {"ddz" "doudizhu"} id)))
```

## check if all the config value are used.

under easyconf.confs namespace, we supply a function named check.

this function will check whether all the config value be used. If not
, it will throw a example to tell you all the config value that
aren't used.

### You should invoke this function after the bootstrap of the program.
