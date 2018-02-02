(ns museum-of-water.core
  (:require
    [reagent.core :as r]
    [ajax.core :refer [GET POST]]
    [cljs.core.async :as async :refer [put! <! chan]]
    [goog.labs.format.csv :as csv])
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:import [goog.async Debouncer]))

(def state (r/atom nil))
(def search (r/atom nil))
(def scroll (r/atom 0))

(def re-data-ids #"([0-9\.]+?) (.*)")
(def re-audio-links #"href=\"/media/audio/([0-9]+)_(.*?)\"")
(def re-image-links #"href=\"/media/Small%20JPEGS/([^\.]*?).jpg\"")

(def on-tap-click (if (or (.hasOwnProperty js/window "ontouchstart") (.-MaxTouchPoints js/navigator)) :on-click :on-click))

;(js/alert (str "Using tap event: " (name on-tap-click)))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

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
  (reset! search (.. ev -target -value)))

(defn search-input []
  (fn []
    [:input#searchbox {:auto-focus true
                       :value @search
                       :on-change update-search
                       :on-blur #(reset! search nil)}]))

(def search-input-focus (with-meta search-input {:component-did-mount (fn [el] (.focus (r/dom-node el)))}))

(defn event-start [ev]
  (reset! state nil))

(defn event-back [ev]
  (reset! state nil)
  (trigger-stop))

(defn event-clear-search [ev]
  (reset! search ""))

(defn event-reset-search [ev]
  (reset! search nil))

(defn event-play [d ev]
  (reset! state d)
  (trigger-play))

(defn event-scroll-to-top [ev]
  (reset! scroll 0)
  (.scrollTo js/window 0 0))

;; -------------------------
;; Views

(defn header []
  [:div#header
   [:img {:src "img/museum-of-water-wordmark.svg"}]
   [:p "Voices from the"]
   [:p "Western Australian"]
   [:p "Collection"]
   [:p.numbers "#777 – 1041"]])

(defn back-to-top []
  (when (> @scroll 200)
    [:img#back-to-top {:src "img/chevron-circle-up.svg"
                       on-tap-click event-scroll-to-top}]))

(defn home-page [data audio-files image-files]
  (let [selected (get @state 1)
        audio-file (get audio-files selected)]
    [:div#app
     [:audio#player (if audio-file {:src (str "media/audio/" selected "_" audio-file) :auto-play true :controls false} {:src ""})]
     (when selected
       [:div#detail
        [:img#hero {:src (str "media/Small JPEGS/" selected "L.jpg")}]
        [:img#back {:src "img/chevron-circle-left.svg" on-tap-click event-back}]
        (when (nil? audio-file)
          [:img#no-audio {:src "img/microphone-slash.svg"}])])
     [:div
      [header]
      (when (not selected)
        [:span#interface {:class (if (nil? @search) "" "out")}
         [back-to-top]
         (when (not (nil? @search))
           [search-input-focus])
         (if (nil? @search)
           [:img {:src "img/search.svg" on-tap-click event-clear-search}]
           [:img {:src "img/times-circle.svg" on-tap-click event-reset-search}])])
      [:div#bottles
       (doall
         (for [d (if @search (filter (fn [c] (or (= (.indexOf (get c 1) @search) 0) (not= (.indexOf (clojure.string/lower-case (get c 2)) (clojure.string/lower-case @search)) -1))) data) data)]
           (with-meta
             [:span.thumbnail
              [:img {:src (str "media/thumbnails/" (get d 1) "L.jpg")
                     on-tap-click (partial event-play d)}]
              [:p.number (get d 1)]
              [:p.attribution (get d 2)]]
             {:key (get d 1)})))]]]))

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
  (.addEventListener
    js/window
    "scroll"
    (debounce 
      (fn [ev]
        (reset! scroll (.. js/window -scrollY)))
      100))
  (mount-root))
