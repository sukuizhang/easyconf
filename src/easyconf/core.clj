(ns easyconf.core
  (:require [easyconf.confs :as confs]
   [clojure.tools.namespace :as namespace]))

(defn config
  "set conf-name to value conf-value, if check-duplicate is ture
   then every conf-name can only be set only once"
  ([conf-name conf-value]
     (config config-name conf-value false))
  ([conf-name value check-duplicate]
     (if (and check-duplicate (find @conf-vals conf-name))
       (throw (RuntimeException.
               (str "conf-name : " conf-name " duplicated")))
       (confs/change-value conf-name value))))

(defn config-once
  "convenient function to force every conf-name can only be configured once,
   otherwiese will cause a exception"
  [conf-name conf-value]
  (config conf-name conf-value true))

(defmacro defconf
  "use it just like (def ...), and it will inject config value to def var."
  [& form]
  `(do
     (let [var# (def ~@form)]
         (easyconf.confs/add-var! var#) var#)))

(defmacro defnconf
  "use it just like (defn ...), and it will inject config value to def var."
  [& form]
  `(do
     (let [var# (defn ~@form)]
         (easyconf.confs/add-var! var#) var#)))

;;all configure file must be put under easyconf.conf namespace
;;preload to guarantee the config value be loaded first.
(confs/load-ns 'easyconf.conf)
