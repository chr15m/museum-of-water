(ns museum-of-water.core
  (:require
    [reagent.core :as r]
    [ajax.core :refer [GET POST]]
    [cljs.core.async :as async :refer [put! <! chan]]
    [goog.labs.format.csv :as csv])
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]]))

(def state (r/atom nil))
(def search (r/atom nil))

(def re-data-ids #"([0-9\.]+?) (.*)")
(def re-audio-links #"href=\"/media/audio/([0-9]+)_(.*?)\"")
(def re-image-links #"href=\"/media/Small%20JPEGS/([^\.]*?).jpg\"")

(def on-tap-click (if (or (.hasOwnProperty js/window "ontouchstart") (.-MaxTouchPoints js/navigator)) :on-click :on-click))

;(js/alert (str "Using tap event: " (name on-tap-click)))

(defn trigger-play []
  (let [player (js/document.getElementById "player")]
    (js/setTimeout
      (.play player)
      100)))

(defn trigger-stop []
  (.pause (js/document.getElementById "player")))

(defn get-file [url]
  (let [c (chan)]
    (GET url {:handler #(put! c %) :error-handler js/alert})
    c))

(def re-numerics (js/RegExp. "\\D" "g"))

(defn update-search [ev]
  (js/console.log (.. ev -target -value))
  (let [v (.. ev -target -value)
        v-clean (.replace v re-numerics "")]
    (print v v-clean)
    (reset! search v-clean)))

(defn search-input []
  (fn []
    [:input#searchbox {:auto-focus true
             :on-change update-search :value @search :type "number"
             :on-blur #(reset! search nil)}]))

(def search-input-focus (with-meta search-input {:component-did-mount (fn [el] (.focus (r/dom-node el)))}))

(defn event-start [ev]
  (reset! state true))

(defn event-back [ev]
  (reset! state true)
  (trigger-stop))

(defn event-clear-search [ev]
  (reset! search ""))

(defn event-reset-search [ev]
  (reset! search nil))

(defn event-play [d ev]
  (reset! state d)
  (trigger-play))

;; -------------------------
;; Views

(defn header []
  [:div#header
   [:img {:src "img/museum-of-water-wordmark.svg" :class (if (not (nil? @search)) "invisible")}]
   [:p "Voices from the"]
   [:p "Western Australian"]
   [:p "Collection"]
   [:p.numbers "#777 – 1041"]])

(defn home-page [data audio-files image-files]
  (let [selected (get @state 1)
        audio-file (get audio-files selected)]
    (if (nil? @state)
      [:div#app
       [header]
       [:img#enter {:src "img/chevron-circle-down.svg" on-tap-click event-start}]]
      [:div#app
       [:audio#player (if audio-file {:src (str "media/audio/" selected "_" audio-file) :auto-play true :controls false} {:src ""})]
       (when selected
         [:div#detail
          [:img#hero {:src (str "media/Small JPEGS/" selected "L.jpg")}]
          [:img#back {:src "img/chevron-circle-left.svg" on-tap-click event-back}]
          (when (nil? audio-file)
            [:img#no-audio {:src "img/microphone-slash.svg"}])])
       [:div {:class (if (not (= @state true)) "hidden" "")}
        [header]
        [:span#search
         (when (not (nil? @search))
           [search-input-focus])
         (if (nil? @search)
           [:img {:src "img/search.svg" on-tap-click event-clear-search}]
           [:img {:src "img/times-circle.svg" on-tap-click event-reset-search}])]
        [:div#bottles
         (doall
           (for [d (if @search (filter (fn [c] (= (.indexOf (get c 1) @search) 0)) data) data)]
             (with-meta
               [:span.thumbnail
                [:img {:src (str "media/thumbnails/" (get d 1) "L.jpg")
                       on-tap-click (partial event-play d)}]
                [:p (str "#" (get d 1))]
                [:p (get d 2)]]
               {:key (get d 1)})))]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  ; at startup load the asset lists
  (go
    (let [data (filter some? (map #(re-matches re-data-ids (get % 0)) (csv/parse (<! (get-file "media/data-csv.txt")))))
          audio-files (into {} (map (fn [a] {(get a 1) (get a 2)}) (re-seq re-audio-links (<! (get-file "media/audio/")))))
          image-files (map #(second %) (re-seq re-image-links (<! (get-file "media/Small JPEGS/"))))]
      (r/render [home-page data audio-files image-files] (.getElementById js/document "app")))))

(defn init! []
  (mount-root))
