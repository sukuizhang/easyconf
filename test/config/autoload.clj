(ns config.autoload
  (:use [easyconf.core]))

(config-once :conf-1 "conf-1")
(config-once :conf-two "conf-two")
(config-once :game-name (fn [id] (get {"ddz" "doudizhu"} id)))

