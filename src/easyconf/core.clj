(ns easyconf.core
  (:require [easyconf.confs :as confs]))

(defn config
  "set conf-name to value conf-value, if check-duplicate is ture
   then every conf-name can only be set only once"
  ([conf-name value]
     (if (and (confs/config-only-once?) (find @@#'confs/conf-vals conf-name))
       (throw (RuntimeException.
               (str "conf-name : " conf-name " duplicated")))
       (confs/config-value (keyword conf-name) value))))

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
(confs/load-dir (confs/default-conf-path))
