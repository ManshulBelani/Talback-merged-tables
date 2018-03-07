/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.android.talkback.CollectionState;
import com.android.talkback.controller.CursorControllerApp;
import com.android.utils.LogUtils;

import static java.security.AccessController.getContext;

public class PerformActionUtils {
    private static final NodeFilter FILTER_COLLECTION = new NodeFilter() {
        @Override
        public boolean accept(AccessibilityNodeInfoCompat node) {
            int role = Role.getRole(node);
            return role == Role.ROLE_LIST || role == Role.ROLE_GRID;
        }
    };

    public static boolean performAction(AccessibilityNodeInfoCompat node, int action) {
        if (node == null) {
            return false;
        }

        logPerformAction(node, action);
        return node.performAction(action);
    }

    public static boolean performAction(AccessibilityNodeInfoCompat node, int action, Bundle args) {
        if (node == null) {
            return false;
        }

        // We create an instance of tableItemState to get the row index, column index , etc. for the node
        // on which this method was called
//        CollectionState mCollectionState = new CollectionState();
//        mCollectionState.updateCollectionInformation(node, null);
//        CollectionState.TableItemState tableItem = mCollectionState.getTableItemState();
//        int rowIndex = tableItem.getColumnIndex();
//        int columnIndex = tableItem.getRowIndex();

        logPerformAction(node, action);

        CollectionState mCollectionState = new CollectionState();


        if (action==AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT ||
                action==AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT)
        {
            mCollectionState.rowChanged = false;
            return node.performAction(action, args);
        }

        // if control comes here it means action is up or down

        mCollectionState.rowChanged = true;

        AccessibilityNodeInfoCompat announcedNodeParent = node.getParent();
        if (announcedNodeParent==null)
            return false;

        AccessibilityNodeInfoCompat newCollectionRoot= AccessibilityNodeInfoUtils
                .getSelfOrMatchingAncestor(announcedNodeParent, FILTER_COLLECTION);

        if (newCollectionRoot==null)
            return false;

        AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo = newCollectionRoot.getCollectionInfo();
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfo = node.getCollectionItemInfo();

        if (collectionInfo==null || collectionItemInfo==null)
            return false;

        int numColumns = collectionInfo.getColumnCount();
        int numRows = collectionInfo.getRowCount();

        // IN MY PHONE ROW AND COL INDICES ARE SWAPPED
//        int rowIndex = collectionItemInfo.getColumnIndex();
//        int columnIndex = collectionItemInfo.getRowIndex();
//

        int rowIndex = collectionItemInfo.getRowIndex();
        int columnIndex = collectionItemInfo.getColumnIndex();

        ///////////////////////////////////////////////////////
        int rowSpan=collectionItemInfo.getRowSpan();
        int colSpan=collectionItemInfo.getColumnSpan();
        //////////////////////////////////////////////////////


        //Log.d("numrows = ",numRows+"");
        //Log.d("numcols = ",numColumns+"");
        //Log.d("row index = ",rowIndex+"");
        //Log.d("col Index = ",columnIndex+"");

        /////////////////////////////////////////
        Log.d("row span1 = ",rowSpan+"");
        Log.d("col span1 = ",colSpan+"");

        //CharSequence toastText = rowSpan+" "+colSpan;
        //Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
        ////////////////////////////////////////


        final Bundle arg = new Bundle();
        arg.putString(
                AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING, "");

        if (action== CursorControllerApp.HTML_UP) //Upward HTML movement
        {
            if (rowIndex==0)
            {
                // if we are in first row move left as many times as column index + 1
                for(int i=0; i<columnIndex+1; i++){
                    node.performAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT,arg);
                    if (colSpan>1){
                        i=i+colSpan;
                    }
                }
            }
            else {

                for (int i = 0; i < numColumns; i++) {
                    node.performAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT, arg);
                    //i = i + colSpan;  // to handle column span in tables elements
                    if (colSpan>1){
                        i=i+colSpan;
                    }
                }
            }

        }
        else if (action==CursorControllerApp.HTML_DOWN) //Downward HTML movement
        {   Log.d("numrows = ",numRows+"");
            Log.d("numcols = ",numColumns+"");
            if (rowIndex== numRows- 1 )
            {
                // if we are in last row move right as many times as numcolumns - column index
                for(int i=0; i<numColumns - columnIndex ; i++){
                    rowSpan=node.getTraversalBefore().getCollectionItemInfo().getRowSpan();
                    colSpan=collectionItemInfo.getColumnSpan();
                    Log.d("row span = ",rowSpan+"");
                    Log.d("col span = ",colSpan+"");
                    if (colSpan>1){
                        i=i+colSpan-1;
                    }
                    node.performAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT,arg);
                    if (colSpan>1){
                        i=i+colSpan;
                    }
                }
            }
            else {

                for (int i = 0; i < numColumns; i++) {
                    //mCollectionState.updateCollectionInformation(node, null);
                    //CollectionState.TableItemState tableItem = mCollectionState.getTableItemState();
                    //rowSpan=tableItem.getRowSpan();
                    //colSpan=tableItem.getColumnSpan();
                    Log.d("node: ",node+"");
                    Log.d("node after: ",node.getTraversalAfter()+"");
                    rowSpan=collectionItemInfo.getRowSpan();
                    //rowSpan=node.getTraversalAfter().getCollectionItemInfo().getRowSpan();
                    colSpan=collectionItemInfo.getColumnSpan();
                    Log.d("row span = ",rowSpan+"");
                    Log.d("col span = ",colSpan+"");
                    Log.d("row index = ",rowIndex+"");
                    Log.d("col Index = ",columnIndex+"");
                    if (colSpan>1){
                        i=i+colSpan-1;
                    }
                    node.performAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT, arg);
                    //i = i + colSpan ;  // to handle column span in tables elements
                }
            }
        }
//        mCollectionState.rowChanged = false;
        return true;
    }

    private static void logPerformAction(AccessibilityNodeInfoCompat node, int action) {
        LogUtils.log(PerformActionUtils.class, Log.DEBUG, "perform action " + action +
                " for node " + node.toString());
    }
}
