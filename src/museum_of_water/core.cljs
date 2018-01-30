(ns museum-of-water.core
  (:require
    [reagent.core :as r]
    [ajax.core :refer [GET POST]]
    [cljs.core.async :as async :refer [put! <! chan]]
    [goog.labs.format.csv :as csv])
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]]))

(def current (r/atom nil))

(def re-audio-links #"href=\"/media/MP3/._(.*?)\"")
(def re-image-links #"href=\"/media/Small%20JPEGS/([^\.]*?).jpg\"")

(defn get-file [url]
  (let [c (chan)]
    (GET url {:handler #(put! c %)
                               :error-handler js/alert})
    c))

;; -------------------------
;; Views

(defn home-page [data audio-files image-files]
  [:div#app 
   (when @current
     [:div
      [:img#hero {:src (str "media/Small JPEGS/" (get @current 1) "L.jpg")}]
      [:img#back {:src "img/chevron-circle-left.svg" :on-click #(reset! current nil)}]])
   [:div {:class (if @current "hidden" "")}
    [:img#logo {:src "img/museum-of-water-wordmark.svg"}]
    [:div#bottles
     (for [d data]
       (with-meta
         [:span.thumbnail
          [:img {:src (str "media/thumbnails/" (get d 1) "L.jpg")
                 :on-click (fn [ev] (print d) (reset! current d))}]
          (get d 1)]
         {:key (get d 1)}))]]])

;; -------------------------
;; Initialize app

(defn mount-root []
  ; at startup load the asset lists
  (go
    (let [data (filter #(= (get % 6) "Yes") (csv/parse (<! (get-file "media/data-csv.txt"))))
          audio-files (map #(second %) (re-seq re-audio-links (<! (get-file "media/MP3/"))))
          image-files (map #(second %) (re-seq re-image-links (<! (get-file "media/Small JPEGS/"))))]
      (print data)
      (print audio-files)
      (print image-files)
      (r/render [home-page data] (.getElementById js/document "app")))))

(defn init! []
  (mount-root))
