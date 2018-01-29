(ns museum-of-water.prod
  (:require
    [museum-of-water.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
