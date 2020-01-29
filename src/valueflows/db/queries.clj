;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.db.queries
  (:require [valueflows.stores :refer [stores]]
            [clj-storage.core :as store]

            [taoensso.timbre :as log]
            [clojure.pprint :as pp]

            ;; depth first search
            [clojure.walk :refer [postwalk]]))

(def not-nil? (complement nil?))

(defn query-process
  "This will return all processes if no arguments or will fetch on process by id"
  ([]
   (let [processes (store/query (:process-store stores) {})]
     (map #(let [inputs (store/query (:transaction-store stores) {:inputOf (:processId %)})
                 outputs (store/query (:transaction-store stores) {:outputOf (:processId %)})
                 ]
             (assoc %
                    :inputs inputs
                    :outputs outputs)
             )
            processes)
     )
   )
  (
   [{:keys [processId]}]
   (when processId
     (let [process (store/query (:process-store stores) {:processId processId})
           inputs (store/query (:transaction-store stores) {:inputOf processId})
           outputs (store/query (:transaction-store stores) {:outputOf processId})]
       processId
       (if (empty? process)
         nil
         (assoc (-> process
                    first)
                :inputs inputs
                :outputs outputs
                ))))))

(defn query-economic-event
  "This will return all eonomic events if no parameters are passed or will filter economic events by some criteria"
  ([]
   (let [allEvents (map #(assoc %
                                :inputOf (query-process {:processId (:inputOf %)})
                                :outputOf (query-process {:processId (:outputOf %)})
                                ) (store/query (:transaction-store stores) {}))
         ]
     allEvents

     )
   )
  ([{:keys [economicEventId before after provider receiver action inputOf outputOf resourceInventoryAs resourceConformsTo toResourceInventoriedAs] :as param-map}]
   (let [economicEvent (-> (store/query (:transaction-store stores) param-map)
                           last)
         inputProcess (query-process {:processId (:inputOf economicEvent)})
         outputProcess (query-process {:processId (:outputOf economicEvent)})
         ]
     (assoc economicEvent
            :inputOf inputProcess
            :outputOf outputProcess
            )
     )
   )
  )


;; Get-all-resources []
;; [{:name :quantity :location :unit :tags(conforms-to)}]

(defn query-resource
  [{:keys [name]}]
  "This function returns information of a given resource if it exists and nil otherwise"
  (when name
    (let [resource (store/query (:transaction-store stores) {:resourceInventoriedAs name})
          toResource (store/query (:transaction-store stores) {:toResourceInventoriedAs name})
          transferred (store/aggregate (:transaction-store stores) [{"$match" {:toResourceInventoriedAs name}}
                                                                 {"$group" {:_id "$receiver"
                                                                            :resourceQuantityHasNumericalValue {"$sum" "$resourceQuantityHasNumericalValue"}}}])
          provided (store/aggregate (:transaction-store stores) [{"$match" {:resourceInventoriedAs name}}
                                                                 {"$group" {:_id "$provider"
                                                                            :resourceQuantityHasNumericalValue {"$sum" "$resourceQuantityHasNumericalValue"}}}])
          resources (distinct (into resource toResource))
          ]
      (when-not (empty? resources)
        {:resourceQuantityHasNumericalValue  (if (some #(= (:action %) "transfer") resources)
                                               (- (if (empty? transferred)
                                                    0
                                                    (int (:resourceQuantityHasNumericalValue (first transferred)))
                                                    )
                                                  (if (empty? provided)
                                                    0
                                                    (int (:resourceQuantityHasNumericalValue (first provided)))
                                                    ))
                                               (- (if (empty? provided)
                                                    0
                                                    (int (:resourceQuantityHasNumericalValue (first provided)))
                                                    )
                                                  (if (empty? transferred)
                                                    0
                                                    (int (:resourceQuantityHasNumericalValue (first transferred)))
                                                    ))
                                               )

         
         :resourceQuantityHasUnit (-> resource
                                      last
                                      :resourceQuantityHasUnit)
         :name name
         :resourceId name
         :currentLocation (-> resource
                              last
                              :currentLocation)}
        )
      )))

(defn list-all-resources []
  (let [economic-events (store/query (:transaction-store stores) {})
        resourceInventoriedAs (filter #(and (:resourceInventoriedAs %) (not-nil? (:resourceInventoriedAs %))) economic-events)
        toResourceInventoriedAs (filter #(and (:toResourceInventoriedAs %) (not-nil? (:toResourceInventoriedAs %))) economic-events)]
    (->> (concat resourceInventoriedAs toResourceInventoriedAs)
         (mapv #(vals (select-keys % [:toResourceInventoriedAs :resourceInventoriedAs])))
         (apply concat)
         set
         (remove nil?)
         (map #(query-resource {:name %}))
         (remove nil?))
    
    #_(remove nil? (set (apply concat (mapv #(vals (select-keys % [:toResourceInventoriedAs :resourceInventoriedAs])) (concat resourceInventoriedAs toResourceInventoriedAs)))))))

(defn query-intent
  [{:keys [provider]}]
  "Returns a list of intents, currently searching only by provider"
  (let [intents (store/query (:intent-store stores) {:provider provider})]
    intents))

(defn economic-event->process [economic-event-id]
  (let [economic-event (query-economic-event {:economicEventId economic-event-id})
        input-process (:inputOf economic-event)
        output-process (:outputOf economic-event)]
    (if (and input-process output-process)
      (throw (Exception. "An event cannot be both an input and output of a process."))
      (or input-process output-process))))


;; The below functions are taken from https://valueflo.ws/appendix/track.html (before=traceback)
(defn traceback-process
  [process-id]
  "Only one step back"
  (let [inputs (:inputs (query-process {:processId process-id}))]
    (mapv #(select-keys % [:action :economicEventId :provider :resourceQuantityHasNumericalValue :resourceQuantityHasUnit :receiver]) inputs)))

;; TODO: check if always enough
(defn traceback-economic-resource
  [resource-name]
  "Only one step back"
  (let [resource (store/query (:transaction-store stores) {:resourceInventoriedAs resource-name})
        toResource (store/query (:transaction-store stores) {:toResourceInventoriedAs resource-name})
        resources (distinct (into resource toResource))
        ]
    (mapv #(select-keys % [:action 
                           :economicEventId 
                           :provider
                           :receiver
                           :resourceQuantityHasNumericalValue
                           :resourceQuantityHasUnit
                           ]) resources
          #_(dissoc (query-economic-event {:resourceInventoriedAs resource-name})
            :hasPointInTime
            :inputOf
            :outputOf
            :note
            :currentLocation
            :satisfies 
            :toResourceInventoriedAs
            :resourceClassifiedAs
            :resourceInventoriedAs))

    ))

(defn traceback-economic-event
  [economic-event-id]
  "One level traceback"
  (let [economic-event (query-economic-event {:economicEventId economic-event-id})]
    (if (some? (:outputOf economic-event))
      ;; this is a process
      
      (select-keys (:outputOf economic-event)
                   [:name :processId])
      ;; this is a resource
      (if (some? (:inputOf economic-event))
        (select-keys (query-resource {:name (:resourceInventoriedAs economic-event)})
                     [:name 
                      :resourceQuantityHasNumericalValue
                      :resourceQuantityHasUnit
                      :resourceId]
                     )
        ;; this is a resource
        (if (= (:action economic-event) "transfer")
          (select-keys (query-resource {:name (:resourceInventoriedAs economic-event)})
                       [:name 
                        :resourceQuantityHasNumericalValue
                        :resourceQuantityHasUnit
                        :resourceId]
                       )
          nil)))))

(defn trace-resource
  [resource-name]
  "Given a resource name the whole backtrace tree is returned or nil if the resource doesnt exist"
  (let [economic-resource (dissoc (query-resource {:name resource-name})
                                  :currentLocation)]
    (loop [trace-tree [economic-resource]]
      (let [leaf (last trace-tree)
            result (atom nil)]
        (when leaf
          (cond
            (not-nil? (:processId leaf)) (reset! result (mapv #(traceback-process (:processId %)) leaf) )
            (not-nil? (:economicEventId leaf)) (reset! result (traceback-economic-event (:economicEventId leaf)))
            (not-nil? (:resourceId leaf)) (reset! result (traceback-economic-resource (:name leaf)))
            (some? leaf) (reset! result (mapv #(traceback-economic-event (:economicEventId %)) leaf))
            :else nil)
          (if @result 
            (recur (conj trace-tree @result))
            trace-tree))))))


;; track-resource [name]
;; [{EconomicEvent}{Process}[{EconomicEvent}]]
;; [{:economicevent {} :process {}} {:economicEvent - Tree structure]
#_(defn track-resource [resource-name]
  "This function takes a resource name and return the nested data structure representing the back-tracking of this resource."
  (let [counter (atom -1)
      line-counter (atom 0)
      print-touch (fn [x]
                    (print (swap! line-counter inc) ":" (pr-str x) "→ "))
      change (fn [x]
               (let [new-x (swap! counter inc)]
                 (prn new-x)
                 [new-x x]))]
  (postwalk (fn [x]
              (print-touch x)
              (change x))
            {:economic-event (query-economic-event {:toResourceInventoriedAs resource-name})}
            {:a 1 :b 2}))
  (loop [economic-event (query-economic-event {:toResourceInventoriedAs resource-name})
         tracking []]
    (if-not (and economic-event
                 (:output-of economic-event))
      tracking)
    (let [process (query-process {:processId (:outputOf economic-event)})]
      (recur ))
  #_(if (zero? n)
    accumulator  ; we're done
    (recur (dec n) (* accumulator n)))))



