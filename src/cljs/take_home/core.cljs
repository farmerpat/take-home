(ns take-home.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [take-home.ajax :as ajax]
    [take-home.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))

(def repo-name (r/atom ""))

(defn text-input []
  [:div.repo-input
   [:label "Repo:"]
   [:input {:type "text"
            :value @repo-name
            :on-change #(reset! repo-name (.-value (.-target %)))
            :placeholder "Repo"}]
   [:button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (rf/dispatch [:submit-search @repo-name]))}
    "Search"]])

;; (defn repo-list []
;;   (let [repos @(rf/subscribe [:repo-list])]
;;     (println "after repo-list let")
;;     (clj->js (println repos))
;;     (fn []
;;       ;; then foreach repo add it to the div or what not
;;       ;;(println "i r repo-list with repos")
;;       [:div
;;        [:h1 "Repo List"]
;;        [:ul
;;         (map (fn [{:keys [release-date name owner]}]
;;                [:li {:key name} (str owner "/" name)])
;;              repos)]])))

(defn repo-list []
  (let [repos @(rf/subscribe [:repo-list])]
    (println "after repo-list let")
    (clj->js (println repos))
    (fn []
      ;; then foreach repo add it to the div or what not
      ;;(println "i r repo-list with repos")
      [:div
       [:h1 "Repo List"]
       (into [:ul] (map #(vector :li (str (:owner %) "/" (:name %))) repos))
       ;; [:ul
       ;;  (map (fn [{:keys [release-date name owner]}]
       ;;         [:li {:key name} (str owner "/" name)])
       ;;       repos)]
       ])))

(defn home-page []
  (println "i r home-page with repolists")
  [:section.section>div.container>div.content
   [text-input]
   [repo-list]])

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:init-db])
  (ajax/load-interceptors!)
  (mount-components))
