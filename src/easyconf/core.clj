(ns easyconf.core
  (:require [easyconf.confs :as confs]
            [easyconf.load :as load]))

(defmacro defconf
  [& form]
  `(do
     (easyconf.load/load-conf-path)
     (let [var# (def ~@form)]
         (easyconf.confs/add-var! var#) var#)))
