(ns easyconf.core
  (:require [easyconf.confs :as confs]
            [easyconf.load :as load]))

(defmacro defconf
  "use it just like (def ...), and it will inject config value to def var."
  [& form]
  `(do
     (easyconf.load/load-conf-path)
     (let [var# (def ~@form)]
         (easyconf.confs/add-var! var#) var#)))

(defmacro defnconf
  "use it just like (defn ...), and it will inject config value to def var."
  [& form]
  `(do
     (easyconf.load/load-conf-path)
     (let [var# (defn ~@form)]
         (easyconf.confs/add-var! var#) var#)))
