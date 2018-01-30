(ns museum-of-water.core
  (:require
    [reagent.core :as r]
    [ajax.core :refer [GET POST]]
    [cljs.core.async :as async :refer [put! <! chan]]
    [goog.labs.format.csv :as csv])
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]]))

(def state (r/atom nil))

(def re-audio-links #"href=\"/media/audio/([0-9]+)_(.*?)\"")
(def re-image-links #"href=\"/media/Small%20JPEGS/([^\.]*?).jpg\"")

(defn trigger-play []
  (let [player (js/document.getElementById "player")]
    (js/setTimeout
      (.play player)
      1000)))

(defn trigger-stop []
  (.pause (js/document.getElementById "player")))

(defn get-file [url]
  (let [c (chan)]
    (GET url {:handler #(put! c %) :error-handler js/alert})
    c))

;; -------------------------
;; Views

(defn home-page [data audio-files image-files]
  (let [selected (get @state 1)
        audio-file (get audio-files selected)]
    [:div#app
     [:audio#player (if audio-file {:src (str "media/audio/" selected "_" audio-file) :auto-play true :controls false} {:src ""})]
     (when selected
       [:div#detail
        [:img#hero {:src (str "media/Small JPEGS/" selected "L.jpg")}]
        [:img#back {:src "img/chevron-circle-left.svg" :on-click (fn [ev] (reset! state nil) (trigger-stop))}]
        (when (nil? audio-file)
          [:img#no-audio {:src "img/microphone-slash.svg"}])])
     [:div {:class (if @state "hidden" "")}
      [:img#logo {:src "img/museum-of-water-wordmark.svg"}]
      [:div#bottles
       (for [d data]
         (with-meta
           [:span.thumbnail
            [:img {:src (str "media/thumbnails/" (get d 1) "L.jpg")
                   :on-click (fn [ev] (reset! state d) (trigger-play))}]
            (get d 1)]
           {:key (get d 1)}))]]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  ; at startup load the asset lists
  (go
    (let [data (filter #(= (get % 6) "Yes") (csv/parse (<! (get-file "media/data-csv.txt"))))
          audio-files (into {} (map (fn [a] {(get a 1) (get a 2)}) (re-seq re-audio-links (<! (get-file "media/audio/")))))
          image-files (map #(second %) (re-seq re-image-links (<! (get-file "media/Small JPEGS/"))))]
      (r/render [home-page data audio-files image-files] (.getElementById js/document "app")))))

(defn init! []
  (mount-root))
