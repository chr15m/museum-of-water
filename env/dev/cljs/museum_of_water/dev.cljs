(ns ^:figwheel-no-load museum-of-water.dev
  (:require
    [museum-of-water.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
