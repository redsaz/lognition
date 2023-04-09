"use strict";
/*
 Copyright 2023 Redsaz <redsaz@gmail.com>.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

/**
 * Find all menus which link to the specified content-pane.
 * @param contentPaneElement HTMLElement for a content-pane.
 * @returns list of 0 or more menus which contain a link to the content-pane.
 */
function getMenusWithContentPane(contentPaneElement) {
    const menus = [];
    const hashId = "#" + contentPaneElement.id;
    // Find pure-menu-lists which have an anchor hash for id of this content-pane
    // (look for anchor tag with href for id, then find parent li, then ul.)
    for (const anchor of document.getElementsByTagName("A")) {
        if (anchor.hash === hashId) {
          let candidate = anchor.parentElement;
          while (candidate !== undefined && candidate !== null) {
              if (candidate.tagName === "UL" && candidate.classList.contains("pure-menu-list")) {
                  menus.push(candidate);
                  break;
              }
              candidate = candidate.parentElement;
          }
        }
    }
    
    return menus;
}

function isContentPane(element) {
    return element.classList.contains("content-pane");
}

function activateContentPane(contentPane) {
    let changed = false;
    // Activate by removing the class that marks it inactive.
    if (contentPane !== null
            && contentPane !== undefined
            && contentPane.classList.contains("content-pane-inactive")) {
        changed = true;
        contentPane.classList.remove("content-pane-inactive");
        // This allows dygraph to know to resize to fit to view
        window.dispatchEvent(new Event('resize'));
    }
    return changed;
}

function inactivateContentPane(contentPane) {
    // Activate by removing the class that marks it inactive.
    if (contentPane !== null && contentPane !== undefined) {
        contentPane.classList.add("content-pane-inactive");
    }
}

/**
 * If the element is contained by one or more content-panes, those panes will be selected and
 * active, and the siblings hidden.
 * 
 * A sibling content-pane is determined by looking at the pure-menu-list which lists ids to content
 * panes, one of which may include the id to the content-pane containing the element. All of the
 * other content-panes in that list will be made inactive.
 * @param element HTMLElement that should be shown.
 * @returns true if any content-panes were revealed that weren't previously revealed
 */
function selectContentPanesForElement(element) {
    let changed = false;
    if (element === undefined || element === null) {
        return changed;
    }
    if (isContentPane(element)) {
        changed = activateContentPane(element);
        
        const hashId = "#" + element.id;
        for (const menu of getMenusWithContentPane(element)) {
            // For each menu with an item for the content-pane, select the matching li, unselect
            // the rest, and hide the other content-panes.
            for (const menuItem of menu.getElementsByTagName("LI")) {
                let matchingAnc = false;
                for (const itemLink of menuItem.getElementsByTagName("A")) {
                    if (itemLink.hash === hashId) {
                        matchingAnc = true;
                    } else {
                        const targetContentPane = document.getElementById(itemLink.hash.substring(1));
                        if (targetContentPane !== element) {
                            inactivateContentPane(targetContentPane);
                        }
                    }
                }
                if (matchingAnc === true) {
                    menuItem.classList.add("pure-menu-selected");
                } else {
                    menuItem.classList.remove("pure-menu-selected");
                }
            }
        }
    }
    // In case this element wasn't a content-pane, or if this WAS a content-pane but is contained
    // in a parent content-pane, then we must work back up the content tree until all parent
    // content panes are selected.
    return changed || selectContentPanesForElement(element.parentElement);
}

function selectContentPanesForHash() {
        /*
         * When the hash of the location changes:
         * 1. Find the element with the id for the hash.
         * 2. If element doen't exist (is null or undefined), we're done. Skip the rest of the
         *    instructions.
         * 3. Find out if that element has a "content-pane" class
         * 4. If so:
         *   4.1 Show that element
         *   4.2 Get id of that element, all elements with "content-pane" class MUST have an id
         *   4.2 Hide all the other siblings of that element with "content-pane" class.
         *   4.3 There will be a "pure-menu-list"-class ul element, with "#{id}" href somewhere.
         *       Find that ul.
         *   4.4 For each of the li that has an anchor element with the matching href, make that
         *       menu item selected. The others should be unselected.
         * 5. Get the parent element. Go back to step 2.
         */
      const id = location.hash.substring(1);
      if (id.length === 0) {
          // If no (or empty) hash, then do nothing.
          return;
      }
      const origin = document.getElementById(id);
      // If a content-pane was revealed by the selection, but and the element identified by the
      // hash is not itself a content-pane, then make the browser scroll to the element.
      // (Should not scroll if it is a content-pane, leave the view where it is).
      let changed = selectContentPanesForElement(origin);
      if (changed && !isContentPane(origin)) {
          origin.scrollIntoView();
      }
}

/**
 * When a webpage first loads, all panes are shown by default, so that if javascript is disabled,
 * the panes will at least still be seen. This will do the hiding of all panes, except for:
 * 
 * - The pane referenced by the first link in a pure-menu-list when no links in the list have
 *   "pure-menu-selected"
 * - Panes who have a link with class "pure-menu-selected" in pure-menu-lists
 * - Panes containing (or the pane referenced by) the hash
 */
function selectDefaultContentPanes() {
    // First, hide all content-panes
    for (const panes of document.getElementsByClassName("content-pane")) {
        inactivateContentPane(panes);
    }

    // Then go through each pure-menu-list. If a link has "pure-menu-selected", then select the
    // referenced hash. If there are no such links, select the first link in the list.
    for (const menu of document.getElementsByClassName("pure-menu-list")) {
        let selectify = null;
        for (const link of menu.getElementsByTagName("A")) {
            if (selectify === null || link.classList.contains("pure-menu-selected")) {
                selectify = link;
            }
        }
        if (selectify !== null) {
            let id = selectify.hash.substring(1);
            if (id.length > 0) {
                selectContentPanesForElement(document.getElementById(id));
            }
        }
    }
    
    // Finally, if there is a hash in the location, reveal it.
    selectContentPanesForHash();
}

function load() {
    selectDefaultContentPanes();
}

(function () {
    window.addEventListener("hashchange", selectContentPanesForHash);
    window.addEventListener("load", load);
}());