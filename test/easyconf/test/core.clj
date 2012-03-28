(ns easyconf.test.core
  (:use [easyconf.core])
  (:use [midje.sweet]))

;;config file be put under easyconf.conf/
;;not define in config file
(defconf conf-default "default")
;;will be override by config file
(defconf conf-1 "default1")
;;using custom conf-name
(defconf ^{:conf-name "conf-two"} conf-2 "default2")
;;define a config function
(defnconf game-name [id]  (str id))

;;add validator
(defconf ^{:validator string?} must-string "1")

(fact
  conf-default => "default"
  conf-1 => "conf-1"
  conf-2 => "conf-two"
  (game-name "ddz") => "doudizhu"
  (config :must-string 3) => (throws RuntimeException)
  (do (config :must-string "3") must-string) => "3")


