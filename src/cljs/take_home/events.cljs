(ns take-home.events
  (:require
    [cljs-time.core :as tc]
    [cljs-time.coerce :as tfc]
    [cljs-time.format :as tf]
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]
    [take-home.db :as db]))

(rf/reg-event-db
 :init-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-db
 :search-failure
 (fn [db [_ _]]
   (js/console.log "FAILURE!")
   db))

(rf/reg-event-db
 :repo-search-success
 (fn
   [db [_ res]]
   (if (every? #(not (= (:name %) (get res "name"))) (:repos db))
     (update-in db [:repos] conj {:release-notes (get res "release-notes")
                                :release-date (tfc/from-string (get res "release-date"))
                                :owner (get res "owner")
                                :name (get res "name")
                                :seen false
                                :selected-for-detail-view false})
     db)))

(rf/reg-event-db
 :repo-remove
 (fn [db [_ repo-name]]
   (let [repos (:repos db)]
     (assoc-in db [:repos] (filter #(not (= (:name %) repo-name)) repos)))))

(rf/reg-event-db
 :repo-view
 (fn [db [_ repo-name]]
   (assoc db :repos (map (fn [repo]
                           (if (= (:name repo) repo-name)
                             (assoc
                              (assoc repo :selected-for-detail-view true)
                              :seen true)
                             (assoc repo :selected-for-detail-view false)))
                         (:repos db)))))

(rf/reg-event-fx
 :submit-search
 (fn
   [{:keys [db]} [_ repo-name]]
   {:http-xhrio {:method :get
                 :uri "/api/repo/searchfull"
                 :params {:repo-name repo-name}
                 :request-format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords true})
                 :on-success [:repo-search-success]
                 :on-failure [:search-failure]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-sub
 :repo-list
 (fn [db _]
   (:repos db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))
