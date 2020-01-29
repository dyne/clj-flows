;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.test.scenarios.track-and-trace
  (:require [midje.sweet :refer [against-background before after facts fact =>]]

            [valueflows
             [stores :refer [stores]]]
            [valueflows.db
             [mutations :as mut]
             [queries :as q]]

            [clj-storage.core :as storage :refer [Store]]
            [clj-time.core :as time]
            monger.joda-time
            [mount.core :as mount]
            [taoensso.timbre :as log]
            ))

(def actors #{:waste-management :retail-shop :textile-lab})
(def units #{:kilo :each :hour})

(against-background [(before :contents (mount/start-with-args {:port 8888}))
                     (after :contents (do
                                        (storage/empty-db-stores! stores)
                                        (mount/stop)))]

                ; WIP
                ; * if an economic event fulfills an intent, needs to be created a satisfaction to record that the intent is fulfilled, but not required for this demo
                ; * We'll not include commitments in this first scenario, we may want to track commitment later
                ; * If an intent involves an exchange , it will require a more complext intent structure, we'll need a proposal that involve 2 intents connected each other
                    (facts "Intents & simple process scenario"
                                        ; Current scenario will allow to:
                        ; - define and satisfy intents
                        ; - Create economic events that affect resources
                        ; - Create Process to group a bunch of economic events related each other
                    ; Some implementations of the scenario are:
                        ; - Show where resources are located on a map
                        ; - Track all the resource flow
                        ; - Broadcast available resources on the network
                        ; - Transfer resources
                           (fact "An unknown agent transfer some textile material to Waste Management"
                                 (mut/create-economic-event :transfer 200 :kilo {
                                                                                 :receiver :waste-management
                                                                                 :economicEventId "test-1"
                                                             :toResourceInventoriedAs "Lot 173 textile material"
                                                             :currentLocation "52.372807,4.8981023"
                                                             :resourceClassifiedAs [:red :cotton]})
                                 (mut/create-economic-event :transfer 200 :kilo {
                                                             :receiver :waste-management
                                                             :toResourceInventoriedAs "Lot 173 textile material"
                                                             :currentLocation "52.372807,4.8981023"
                                                             :resourceClassifiedAs [:red :cotton]})
                                 (mut/create-economic-event :transfer 100 :kilo {
                                                             :provider :waste-management
                                                             :receiver :textile-lab
                                                             :resourceInventoriedAs "Lot 173 textile material"
                                                             :toResourceInventoriedAs "Processed textile"
                                                             :currentLocation "52.372807,4.8981023"
                                                             :resourceClassifiedAs [:red :cotton]})
                                 (-> (q/query-resource {:name "Lot 173 textile material"})
                                     :resourceQuantityHasNumericalValue
                                     ) => 300
                                 (-> (q/query-resource {:name "Lot 173 textile material"})
                                     :currentLocation) => "52.372807,4.8981023"
                                 (-> (q/query-economic-event {:economicEventId "test-1"})
                                     :toResourceInventoriedAs) =>  "Lot 173 textile material"
                                 )

                           (fact "Waste Management broadcasts an offer to transfer Lot 173 textile material"
                                 (mut/create-intent :transer 200 :kilo "Availabe to transfer a lot of red cotton textile material in good condition" {
                                                     :provider :waste-management
                                                     :resource-conforms-to :textile-material
                                                     :resource-classified-as [:red :cotton]
                                                                                                                                                      :at-location "52.372807,4.8981023"})
                                 (-> (q/query-intent {:provider "waste-management"})
                                     first
                                     :resource-conforms-to
                                     ) => "textile-material")

                           (fact "Waste Management transfers part of Lot 173 to Textile Lab"
                                 (mut/create-economic-event :transfer 15 :kilo {
                                                                                :economicEventId "test-2"
                                                                                :provider :waste-management
                                                                                :receiver :textile-lab
                                                                                :satisfies 2
                                                                                :resourceInventoriedAs "Lot 173 textile material"

                                                                                :toResourceInventoriedAs "Raw red cotton"
                                                                                :currentLocation "52.372807,4.8981023"
                                                                                :resourceClassifiedAs [:red :cotton]})
                                 (-> (q/query-economic-event {:economicEventId "test-2"})
                                     :toResourceInventoriedAs) => "Raw red cotton"
                                 (-> (q/query-resource {:name "Raw red cotton"})
                                     :resourceQuantityHasNumericalValue
                                     ) => 15
                                 (-> (q/query-resource {:name "Lot 173 textile material"})
                                     :resourceQuantityHasNumericalValue) => 285)

                           (fact "Textile Lab creates a new process to produce a new pair of jeans"
                                 (mut/create-process "Create a new pair of hyper jeans"
                                                     {:note "Hyper jeans will be produced using the hyperballad design"
                                                      :processId "process-test-1"
                                                      :before (time/now)})
                                 (-> (q/query-process)
                                     last
                                     :name
                                     ) => "Create a new pair of hyper jeans")


                           (fact "A bunch of work is done over the process: citing a design, consuming some materials and manufacturing the jeans, at the end the hyper jeans is produced and put in the inventory"
                                 (let [process-id (-> (q/query-process)
                                                      last
                                                      :processId)]
                                   (mut/create-economic-event
                                    :cite 1 :each
                                    {
                                     :provider :worker
                                     :receiver :textile-lab
                                     :inputOf "process-test-1"
                                     :resourceInventoriedAs "hyperballad design"})

                                   (mut/create-economic-event :consume 10 :kilo {
                                                                                 :provider :worker
                                                                                 :receiver :textile-lab
                                                                                 :inputOf "process-test-1"
                                                                                 :resourceInventoriedAs "Raw red cotton"
                                                                                 })

                                   (mut/create-economic-event :work 4 :hour {
                                                                             :provider :worker
                                                                             :receiver :textile-lab                                                           :hasPointInTime (time/now)
                                                                             :inputOf "process-test-1"
                                                                             :resourceConformsTo "clothes manufacturing"
                                                                             :note "Done all the work!"})

                                   (mut/create-economic-event :produce 1 :each {
                                                                                :provider :textile-lab
                                                                                :receiver :textile-lab
                                                                                :outputOf "process-test-1"
                                                                                :currentLocation "TextileLab place"
                                                                                :resourceClassifiedAs [:red :jeans :cotton :slim-fit]
                                                                                :resourceInventoriedAs "hyper jeans"})

                                   ;; Check if the process includes all the inputs
                                   (count (:inputs (select-keys (q/query-process {:processId "process-test-1"}) [:inputs])))
                                   => (q/query-process {:processId "process-test-1"})

                                   ;; Check if the process includes all the outpts
                                   (count (:outputs (select-keys (q/query-process {:processId "process-test-1"}) [:outputs])))
                                   => 1
                                   )


                                 )

))
;; => true

