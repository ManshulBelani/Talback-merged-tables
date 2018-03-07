package com.android.switchaccess.test;

import android.R.integer;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityNodeInfo.CollectionInfo;
import android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo;
import android.view.accessibility.AccessibilityNodeInfo.RangeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Shadow of {@link android.view.accessibility.AccessibilityNodeInfo} that allows a test to set
 * properties that are locked in the original class. It also keeps track of calls to
 * {@code obtain()} and {@code recycle()} to look for bugs that mismatches.
 */
@Implements(AccessibilityNodeInfo.class)
public class ShadowAccessibilityNodeInfo {
    // Map of obtained instances of the class along with stack traces of how they were obtained
    private static final Map<StrictEqualityNodeWrapper, StackTraceElement[]> obtainedInstances =
            new HashMap<>();

    private static final SparseArray<StrictEqualityNodeWrapper> orderedInstances = new SparseArray<>();

    // Bitmasks for actions
    public static final int UNDEFINED_SELECTION_INDEX = -1;

    public static final Parcelable.Creator<AccessibilityNodeInfo> CREATOR =
            new Parcelable.Creator<AccessibilityNodeInfo>() {

                @Override
                public AccessibilityNodeInfo createFromParcel(Parcel source) {
                    return obtain(orderedInstances.get(source.readInt()).mInfo);
                }

                @Override
                public AccessibilityNodeInfo[] newArray(int size) {
                    return new AccessibilityNodeInfo[size];
                }};

    private static int sAllocationCount = 0;

    private static final int CLICKABLE_MASK = 0x00000001;

    private static final int LONGCLICKABLE_MASK = 0x00000002;

    private static final int FOCUSABLE_MASK = 0x00000004;

    private static final int FOCUSED_MASK = 0x00000008;

    private static final int VISIBLE_TO_USER_MASK = 0x00000010;

    private static final int SCROLLABLE_MASK = 0x00000020;

    private static final int PASTEABLE_MASK = 0x00000040;

    private static final int EDITABLE_MASK = 0x00000080;

    private static final int TEXT_SELECTION_SETABLE_MASK = 0x00000100;

    private static final int CHECKABLE_MASK = 0x00001000; //14

    private static final int CHECKED_MASK = 0x00002000; //14

    private static final int ENABLED_MASK = 0x00010000; //14

    private static final int PASSWORD_MASK = 0x00040000; //14

    private static final int SELECTED_MASK = 0x00080000; //14

    private static final int A11YFOCUSED_MASK = 0x00000800;  //16
    private static final int MULTILINE_MASK = 0x00020000; //19

    private static final int CONTENT_INVALID_MASK = 0x00004000; //19

    private static final int DISMISSABLE_MASK = 0x00008000; //19

    private static final int CAN_OPEN_POPUP_MASK = 0x00100000; //19

    private List<AccessibilityNodeInfo> children;

    private Rect boundsInScreen = new Rect();

    private Rect boundsInParent = new Rect();

    private List<Pair<Integer, Bundle>> performedActionAndArgsList;

    // In API prior to 21, actions are stored in a flag, after 21 they are stored in array of
    // AccessibilityAction so custom actions can be supported.
    private ArrayList<AccessibilityAction> actionsArray;
    // Storage of flags
    private int propertyFlags;

    private AccessibilityNodeInfo parent;

    private AccessibilityNodeInfo labelFor;

    private AccessibilityNodeInfo labeledBy;

    private View view;

    private CharSequence contentDescription;

    private CharSequence text;

    private CharSequence className;

    private int textSelectionStart = UNDEFINED_SELECTION_INDEX;

    private int textSelectionEnd = UNDEFINED_SELECTION_INDEX;

    private boolean refreshReturnValue = true;

    private int movementGranularities; //16

    private CharSequence packageName; //14
    private String viewIdResourceName; //18
    private CollectionInfo collectionInfo; //19

    private CollectionItemInfo collectionItemInfo; //19

    private int inputType; //19

    private int liveRegion; //19

    private RangeInfo rangeInfo; //19
    private int maxTextLength; //21

    private CharSequence error; //21
    private AccessibilityNodeInfo traversalAfter; //22

    private AccessibilityNodeInfo traversalBefore; //22

    private OnPerformActionListener actionListener;
    private AccessibilityWindowInfo accessibilityWindowInfo;

    @RealObject
    private AccessibilityNodeInfo realAccessibilityNodeInfo;

    public void __constructor__() {
        ReflectionHelpers.setStaticField(AccessibilityNodeInfo.class, "CREATOR", ShadowAccessibilityNodeInfo.CREATOR);
    }

    @Implementation
    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo info) {
        final ShadowAccessibilityNodeInfo shadowInfo =
                ((ShadowAccessibilityNodeInfo) ShadowExtractor.extract(info));
        final AccessibilityNodeInfo obtainedInstance = shadowInfo.getClone();

        sAllocationCount++;
        StrictEqualityNodeWrapper wrapper = new StrictEqualityNodeWrapper(obtainedInstance);
        obtainedInstances.put(wrapper, Thread.currentThread().getStackTrace());
        orderedInstances.put(sAllocationCount, wrapper);
        return obtainedInstance;
    }

    @Implementation
    public static AccessibilityNodeInfo obtain(View view) {
        // We explicitly avoid allocating the AccessibilityNodeInfo from the actual pool by using the
        // private constructor. Not doing so affects test suites which use both shadow and
        // non-shadow objects.
        final AccessibilityNodeInfo obtainedInstance =
                ReflectionHelpers.callConstructor(AccessibilityNodeInfo.class);
        final ShadowAccessibilityNodeInfo shadowObtained =
                ((ShadowAccessibilityNodeInfo) ShadowExtractor.extract(obtainedInstance));
    /*
     * We keep a separate list of actions for each object newly obtained
     * from a view, and perform a shallow copy during getClone. That way the
     * list of actions performed contains all actions performed on the view
     * by the tree of nodes initialized from it. Note that initializing two
     * nodes with the same view will not merge the two lists, as so the list
     * of performed actions will not contain all actions performed on the
     * underlying view.
     */
        shadowObtained.performedActionAndArgsList = new LinkedList<>();

        shadowObtained.view = view;
        sAllocationCount++;
        StrictEqualityNodeWrapper wrapper = new StrictEqualityNodeWrapper(obtainedInstance);
        obtainedInstances.put(wrapper, Thread.currentThread().getStackTrace());
        orderedInstances.put(sAllocationCount, wrapper);
        return obtainedInstance;
    }

    @Implementation
    public static AccessibilityNodeInfo obtain() {
        return obtain(new View(RuntimeEnvironment.application.getApplicationContext()));
    }

    @Implementation
    public static AccessibilityNodeInfo obtain(View root, int virtualDescendantId) {
        AccessibilityNodeInfo node = obtain(root);
        return node;
    }

    /**
     * Check for leaked objects that were {@code obtain}ed but never
     * {@code recycle}d.
     *
     * @param printUnrecycledNodesToSystemErr - if true, stack traces of calls
     *        to {@code obtain} that lack matching calls to {@code recycle} are
     *        dumped to System.err.
     * @return {@code true} if there are unrecycled nodes
     */
    public static boolean areThereUnrecycledNodes(boolean printUnrecycledNodesToSystemErr) {
        if (printUnrecycledNodesToSystemErr) {
            for (final StrictEqualityNodeWrapper wrapper : obtainedInstances.keySet()) {
                final ShadowAccessibilityNodeInfo shadow =
                        ((ShadowAccessibilityNodeInfo) ShadowExtractor.extract(wrapper.mInfo));

                System.err.println(String.format(
                        "Leaked contentDescription = %s. Stack trace:", shadow.getContentDescription()));
                for (final StackTraceElement stackTraceElement : obtainedInstances.get(wrapper)) {
                    System.err.println(stackTraceElement.toString());
                }
            }
        }

        return (obtainedInstances.size() != 0);
    }

    /**
     * Clear list of obtained instance objects. {@code areThereUnrecycledNodes}
     * will always return false if called immediately afterwards.
     */
    public static void resetObtainedInstances() {
        obtainedInstances.clear();
        orderedInstances.clear();
    }

    @Implementation
    public void recycle() {
        final StrictEqualityNodeWrapper wrapper =
                new StrictEqualityNodeWrapper(realAccessibilityNodeInfo);
        if (!obtainedInstances.containsKey(wrapper)) {
            throw new IllegalStateException();
        }

        if (labelFor != null) {
            labelFor.recycle();
        }

        if (labeledBy != null) {
            labeledBy.recycle();
        }
            if (traversalAfter != null) {
                traversalAfter.recycle();
            }

        if (traversalBefore != null) {
            traversalBefore.recycle();
        }
        obtainedInstances.remove(wrapper);
        int keyOfWrapper = -1;
        for (int i = 0; i < orderedInstances.size(); i++) {
            int key = orderedInstances.keyAt(i);
            if (orderedInstances.get(key).equals(wrapper)) {
                keyOfWrapper = key;
                break;
            }
        }
        orderedInstances.remove(keyOfWrapper);
    }

    @Implementation
    public int getChildCount() {
        if (children == null) {
            return 0;
        }

        return children.size();
    }

    @Implementation
    public AccessibilityNodeInfo getChild(int index) {
        if (children == null) {
            return null;
        }

        final AccessibilityNodeInfo child = children.get(index);
        if (child == null) {
            return null;
        }

        return obtain(child);
    }

    @Implementation
    public AccessibilityNodeInfo getParent() {
        if (parent == null) {
            return null;
        }

        return obtain(parent);
    }

    @Implementation
    public boolean refresh() {
        return refreshReturnValue;
    }

    public void setRefreshReturnValue(boolean refreshReturnValue) {
        this.refreshReturnValue = refreshReturnValue;
    }

    @Implementation
    public boolean isClickable() {
        return ((propertyFlags & CLICKABLE_MASK) != 0);
    }

    @Implementation
    public boolean isLongClickable() {
        return ((propertyFlags & LONGCLICKABLE_MASK) != 0);
    }

    @Implementation
    public boolean isFocusable() {
        return ((propertyFlags & FOCUSABLE_MASK) != 0);
    }

    @Implementation
    public boolean isFocused() {
        return ((propertyFlags & FOCUSED_MASK) != 0);
    }

    @Implementation
    public boolean isVisibleToUser() {
        return ((propertyFlags & VISIBLE_TO_USER_MASK) != 0);
    }

    @Implementation
    public boolean isScrollable() {
        return ((propertyFlags & SCROLLABLE_MASK) != 0);
    }

    public boolean isPasteable() {
        return ((propertyFlags & PASTEABLE_MASK) != 0);
    }

    @Implementation
    public boolean isEditable() {
        return ((propertyFlags & EDITABLE_MASK) != 0);
    }

    public boolean isTextSelectionSetable() {
        return ((propertyFlags & TEXT_SELECTION_SETABLE_MASK) != 0);
    }

    @Implementation
    public boolean isCheckable() {
        return ((propertyFlags & CHECKABLE_MASK) != 0);
    }

    @Implementation
    public void setCheckable(boolean checkable) {
        propertyFlags = (propertyFlags & ~CHECKABLE_MASK) |
                (checkable ? CHECKABLE_MASK : 0);
    }

    @Implementation
    public void setChecked(boolean checked) {
        propertyFlags = (propertyFlags & ~CHECKED_MASK) |
                (checked ? CHECKED_MASK : 0);
    }

    @Implementation
    public boolean isChecked() {
        return ((propertyFlags & CHECKED_MASK) != 0);
    }

    @Implementation
    public void setEnabled(boolean enabled) {
        propertyFlags = (propertyFlags & ~ENABLED_MASK) |
                (enabled ? ENABLED_MASK : 0);
    }

    @Implementation
    public boolean isEnabled() {
        return ((propertyFlags & ENABLED_MASK) != 0);
    }

    @Implementation
    public void setPassword(boolean password) {
        propertyFlags = (propertyFlags & ~PASSWORD_MASK) |
                (password ? PASSWORD_MASK : 0);
    }

    @Implementation
    public boolean isPassword() {
        return ((propertyFlags & PASSWORD_MASK) != 0);
    }

    @Implementation
    public void setSelected(boolean selected) {
        propertyFlags = (propertyFlags & ~SELECTED_MASK) |
                (selected ? SELECTED_MASK : 0);
    }

    @Implementation
    public boolean isSelected() {
        return ((propertyFlags & SELECTED_MASK) != 0);
    }

    @Implementation
    public void setAccessibilityFocused(boolean focused) {
        propertyFlags = (propertyFlags & ~A11YFOCUSED_MASK) |
                (focused ? A11YFOCUSED_MASK : 0);
    }

    @Implementation
    public boolean isAccessibilityFocused() {
        return ((propertyFlags & A11YFOCUSED_MASK) != 0);
    }
    @Implementation
    public void setMultiLine(boolean multiLine) {
        propertyFlags = (propertyFlags & ~MULTILINE_MASK) |
                (multiLine ? MULTILINE_MASK : 0);
    }

    @Implementation
    public boolean isMultiLine() {
        return ((propertyFlags & MULTILINE_MASK) != 0);
    }

    @Implementation
    public void setContentInvalid(boolean contentInvalid) {
        propertyFlags = (propertyFlags & ~CONTENT_INVALID_MASK) |
                (contentInvalid ? CONTENT_INVALID_MASK : 0);
    }

    @Implementation
    public boolean isContentInvalid() {
        return ((propertyFlags & CONTENT_INVALID_MASK) != 0);
    }

    @Implementation
    public void setDismissable(boolean dismissable) {
        propertyFlags = (propertyFlags & ~DISMISSABLE_MASK) |
                (dismissable ? DISMISSABLE_MASK : 0);
    }

    @Implementation
    public boolean isDismissable() {
        return ((propertyFlags & DISMISSABLE_MASK) != 0);
    }

    @Implementation
    public void setCanOpenPopup(boolean opensPopup) {
        propertyFlags = (propertyFlags & ~CAN_OPEN_POPUP_MASK) |
                (opensPopup ? CAN_OPEN_POPUP_MASK : 0);
    }

    @Implementation
    public boolean canOpenPopup() {
        return ((propertyFlags & CAN_OPEN_POPUP_MASK) != 0);
    }

    public void setTextSelectionSetable(boolean isTextSelectionSetable) {
        propertyFlags = (propertyFlags & ~TEXT_SELECTION_SETABLE_MASK) |
                (isTextSelectionSetable ? TEXT_SELECTION_SETABLE_MASK : 0);
    }

    @Implementation
    public void setClickable(boolean isClickable) {
        propertyFlags = (propertyFlags & ~CLICKABLE_MASK) | (isClickable ? CLICKABLE_MASK : 0);
    }

    @Implementation
    public void setLongClickable(boolean isLongClickable) {
        propertyFlags =
                (propertyFlags & ~LONGCLICKABLE_MASK) | (isLongClickable ? LONGCLICKABLE_MASK : 0);
    }

    @Implementation
    public void setFocusable(boolean isFocusable) {
        propertyFlags = (propertyFlags & ~FOCUSABLE_MASK) | (isFocusable ? FOCUSABLE_MASK : 0);
    }

    @Implementation
    public void setFocused(boolean isFocused) {
        propertyFlags = (propertyFlags & ~FOCUSED_MASK) | (isFocused ? FOCUSED_MASK : 0);
    }

    @Implementation
    public void setScrollable(boolean isScrollable) {
        propertyFlags = (propertyFlags & ~SCROLLABLE_MASK) | (isScrollable ? SCROLLABLE_MASK : 0);
    }

    public void setPasteable(boolean isPasteable) {
        propertyFlags = (propertyFlags & ~PASTEABLE_MASK) | (isPasteable ? PASTEABLE_MASK : 0);
    }

    @Implementation
    public void setEditable(boolean isEditable) {
        propertyFlags = (propertyFlags & ~EDITABLE_MASK) | (isEditable ? EDITABLE_MASK : 0);
    }

    @Implementation
    public void setVisibleToUser(boolean isVisibleToUser) {
        propertyFlags =
                (propertyFlags & ~VISIBLE_TO_USER_MASK) | (isVisibleToUser ? VISIBLE_TO_USER_MASK : 0);
    }

    @Implementation
    public void setContentDescription(CharSequence description) {
        contentDescription = description;
    }

    @Implementation
    public CharSequence getContentDescription() {
        return contentDescription;
    }

    @Implementation
    public void setClassName(CharSequence name) {
        className = name;
    }

    @Implementation
    public CharSequence getClassName() {
        return className;
    }

    @Implementation
    public void setText(CharSequence t) {
        text = t;
    }

    @Implementation
    public CharSequence getText() {
        return text;
    }

    @Implementation
    public void setTextSelection(int start, int end) {
        textSelectionStart = start;
        textSelectionEnd = end;
    }

    @Implementation
    public AccessibilityWindowInfo getWindow() {
        return accessibilityWindowInfo;
    }

    public void setAccessibilityWindowInfo(AccessibilityWindowInfo info) {
        accessibilityWindowInfo = info;
    }

    /**
     * Gets the text selection start.
     *
     * @return The text selection start if there is selection or UNDEFINED_SELECTION_INDEX.
     */
    @Implementation
    public int getTextSelectionStart() {
        return textSelectionStart;
    }

    /**
     * Gets the text selection end.
     *
     * @return The text selection end if there is selection or UNDEFINED_SELECTION_INDEX.
     */
    @Implementation
    public int getTextSelectionEnd() {
        return textSelectionEnd;
    }

    @Implementation
    public AccessibilityNodeInfo getLabelFor() {
        if (labelFor == null) {
            return null;
        }

        return obtain(labelFor);
    }

    public void setLabelFor(AccessibilityNodeInfo info) {
        if (labelFor != null) {
            labelFor.recycle();
        }

        labelFor = obtain(info);
    }

    @Implementation
    public AccessibilityNodeInfo getLabeledBy() {
        if (labeledBy == null) {
            return null;
        }

        return obtain(labeledBy);
    }

    public void setLabeledBy(AccessibilityNodeInfo info) {
        if (labeledBy != null) {
            labeledBy.recycle();
        }

        labeledBy = obtain(info);
    }

    @Implementation
    public int getMovementGranularities() {
        return movementGranularities;
    }

    @Implementation
    public void setMovementGranularities(int movementGranularities) {
        this.movementGranularities = movementGranularities;
    }

    @Implementation
    public CharSequence getPackageName() {
        return packageName;
    }

    @Implementation
    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    @Implementation
    public String getViewIdResourceName() {
        return viewIdResourceName;
    }

    @Implementation
    public void setViewIdResourceName(String viewIdResourceName) {
        this.viewIdResourceName = viewIdResourceName;
    }
    @Implementation
    public CollectionInfo getCollectionInfo() {
        return collectionInfo;
    }

    @Implementation
    public void setCollectionInfo(CollectionInfo collectionInfo) {
        this.collectionInfo = collectionInfo;
    }

    @Implementation
    public CollectionItemInfo getCollectionItemInfo() {
        return collectionItemInfo;
    }

    @Implementation
    public void setCollectionItemInfo(CollectionItemInfo collectionItemInfo) {
        this.collectionItemInfo = collectionItemInfo;
    }

    @Implementation
    public int getInputType() {
        return inputType;
    }

    @Implementation
    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    @Implementation
    public int getLiveRegion() {
        return liveRegion;
    }

    @Implementation
    public void setLiveRegion(int liveRegion) {
        this.liveRegion = liveRegion;
    }

    @Implementation
    public RangeInfo getRangeInfo() {
        return rangeInfo;
    }

    @Implementation
    public void setRangeInfo(RangeInfo rangeInfo) {
        this.rangeInfo = rangeInfo;
    }
    @Implementation
    public int getMaxTextLength() {
        return maxTextLength;
    }

    @Implementation
    public void setMaxTextLength(int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }

    @Implementation
    public CharSequence getError() {
        return error;
    }

    @Implementation
    public void setError(CharSequence error) {
        this.error = error;
    }
    @Implementation
    public AccessibilityNodeInfo getTraversalAfter() {
        if (traversalAfter == null) {
            return null;
        }

        return obtain(traversalAfter);
    }

    @Implementation
    public void setTraversalAfter(AccessibilityNodeInfo info) {
        if (this.traversalAfter != null) {
            this.traversalAfter.recycle();
        }

        this.traversalAfter = obtain(info);
    }

    @Implementation
    public AccessibilityNodeInfo getTraversalBefore() {
        if (traversalBefore == null) {
            return null;
        }

        return obtain(traversalBefore);
    }

    @Implementation
    public void setTraversalBefore(AccessibilityNodeInfo info) {
        if (this.traversalBefore != null) {
            this.traversalBefore.recycle();
        }

        this.traversalBefore = obtain(info);
    }

    @Implementation
    public void setSource (View source) {
        this.view = source;
    }

    @Implementation
    public void setSource (View root, int virtualDescendantId) {
        this.view = root;
    }

    @Implementation
    public void getBoundsInScreen(Rect outBounds) {
        if (boundsInScreen == null) {
            boundsInScreen = new Rect();
        }
        outBounds.set(boundsInScreen);
    }

    @Implementation
    public void getBoundsInParent(Rect outBounds) {
        if (boundsInParent == null) {
            boundsInParent = new Rect();
        }
        outBounds.set(boundsInParent);
    }

    @Implementation
    public void setBoundsInScreen(Rect b) {
        if (boundsInScreen == null) {
            boundsInScreen = new Rect(b);
        } else {
            boundsInScreen.set(b);
        }
    }

    @Implementation
    public void setBoundsInParent(Rect b) {
        if (boundsInParent == null) {
            boundsInParent = new Rect(b);
        } else {
            boundsInParent.set(b);
        }
    }

    @Implementation
    public void addAction(int action) {
            if ((action & getActionTypeMaskFromFramework()) != 0) {
                throw new IllegalArgumentException("Action is not a combination of the standard " +
                        "actions: " + action);
            }
        int remainingIds = action;
        while (remainingIds > 0) {
            final int id = 1 << Integer.numberOfTrailingZeros(remainingIds);
            remainingIds &= ~id;
            AccessibilityAction convertedAction = getActionFromIdFromFrameWork(id);
            addAction(convertedAction);
        }
    }

    @Implementation
    public void addAction(AccessibilityAction action) {
        if (action == null) {
            return;
        }

        if (actionsArray == null) {
            actionsArray = new ArrayList<>();
        }
        actionsArray.remove(action);
        actionsArray.add(action);
    }

    @Implementation
    public void removeAction(int action) {
        AccessibilityAction convertedAction = getActionFromIdFromFrameWork(action);
        removeAction(convertedAction);
    }

    @Implementation
    public boolean removeAction(AccessibilityAction action) {
        if (action == null || actionsArray == null) {
            return false;
        }
        return actionsArray.remove(action);
    }
    /**
     * Obtain flags for actions supported. Currently only supports ACTION_CLICK, ACTION_LONG_CLICK,
     * ACTION_SCROLL_FORWARD, ACTION_PASTE, ACTION_FOCUS, ACTION_SET_SELECTION, ACTION_SCROLL_BACKWARD
     * Returned value is derived from the getters.
     *
     * @return Action mask. 0 if no actions supported.
     */
    @Implementation
    public int getActions() {
            int returnValue = 0;
        if (actionsArray == null) {
            return returnValue;
        }

        // Custom actions are only returned by getActionsList
        final int actionSize = actionsArray.size();
        for (int i = 0; i < actionSize; i++) {
            int actionId = actionsArray.get(i).getId();
            if (actionId <= getLastLegacyActionFromFrameWork()) {
                returnValue |= actionId;
            }
        }
        return returnValue;
    }

    @Implementation
    public List<AccessibilityAction> getActionList() {
        if (actionsArray == null) {
            return Collections.emptyList();
        }

        return actionsArray;
    }

    @Implementation
    public boolean performAction(int action) {
        return performAction(action, null);
    }

    @Implementation
    public boolean performAction(int action, Bundle arguments) {
        if (performedActionAndArgsList == null) {
            performedActionAndArgsList = new LinkedList<>();
        }

        performedActionAndArgsList.add(new Pair<Integer, Bundle>(new Integer(action), arguments));
        return (actionListener != null) ? actionListener.onPerformAccessibilityAction(action, arguments)
                : true;
    }

    /**
     * Equality check based on reference equality for mParent and mView and
     * value equality for other fields.
     */
    @Implementation
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AccessibilityNodeInfo)) {
            return false;
        }

        final AccessibilityNodeInfo info = (AccessibilityNodeInfo) object;
        final ShadowAccessibilityNodeInfo otherShadow =
                (ShadowAccessibilityNodeInfo) ShadowExtractor.extract(info);

        boolean areEqual = true;
        if (children == null) {
            areEqual &= (otherShadow.children == null);
        } else {
            //areEqual &= (otherShadow.children != null) && children.equals(otherShadow.children);
        }
        //areEqual &= (parent == otherShadow.parent);

        areEqual &= (propertyFlags == otherShadow.propertyFlags);

        boolean actionsArrayEquality = false;
        if (actionsArray == null && otherShadow.actionsArray == null) {
            actionsArrayEquality = true;
        } else if (actionsArray == null || otherShadow.actionsArray == null) {
            return false;
        } else {
            actionsArrayEquality = actionsArray.equals(otherShadow.actionsArray);
        }
        areEqual &= actionsArrayEquality;
    /*
     * These checks have the potential to become infinite loops if there are
     * loops in the labelFor or labeledBy logic. Rather than deal with this
     * complexity, allow the failure since it will indicate a problem that
     * needs addressing.
     */
        if (labelFor == null) {
            areEqual &= (otherShadow.labelFor == null);
        } else {
            areEqual &= (labelFor.equals(otherShadow.labelFor));
        }

        if (labeledBy == null) {
            areEqual &= (otherShadow.labeledBy == null);
        } else {
            areEqual &= (labeledBy.equals(otherShadow.labeledBy));
        }

        areEqual &= boundsInScreen.equals(otherShadow.boundsInScreen);
        areEqual &= (TextUtils.equals(contentDescription, otherShadow.contentDescription));
        areEqual &= (TextUtils.equals(text, otherShadow.text));

        areEqual &= TextUtils.equals(className, otherShadow.className);
        areEqual &= (view == otherShadow.view);
        areEqual &= (textSelectionStart == otherShadow.textSelectionStart);
        areEqual &= (textSelectionEnd == otherShadow.textSelectionEnd);

        areEqual &= (refreshReturnValue == otherShadow.refreshReturnValue);
        areEqual &= (movementGranularities == otherShadow.movementGranularities);
        areEqual &= (TextUtils.isEmpty(packageName) == TextUtils.isEmpty(otherShadow.packageName));
        if (!TextUtils.isEmpty(packageName)) {
            areEqual &= (packageName.toString().equals(otherShadow.packageName.toString()));
        }
            areEqual &= TextUtils.equals(viewIdResourceName, otherShadow.viewIdResourceName);
            if (collectionInfo == null) {
                areEqual &= (otherShadow.collectionInfo == null);
            } else {
                areEqual &= (collectionInfo.equals(otherShadow.collectionInfo));
            }
        if (collectionItemInfo == null) {
            areEqual &= (otherShadow.collectionItemInfo == null);
        } else {
            areEqual &= (collectionItemInfo.equals(otherShadow.collectionItemInfo));
        }
        areEqual &= (inputType == otherShadow.inputType);
        areEqual &= (liveRegion == otherShadow.liveRegion);
        if (rangeInfo == null) {
            areEqual &= (otherShadow.rangeInfo == null);
        } else {
            areEqual &= (rangeInfo.equals(otherShadow.rangeInfo));
        }
            areEqual &= (maxTextLength == otherShadow.maxTextLength);
        areEqual &= TextUtils.equals(error, otherShadow.error);
            if (traversalAfter == null) {
                areEqual &= (otherShadow.traversalAfter == null);
            } else {
                areEqual &= (traversalAfter.equals(otherShadow.traversalAfter));
            }
        if (traversalBefore == null) {
            areEqual &= (otherShadow.traversalBefore == null);
        } else {
            areEqual &= (traversalBefore.equals(otherShadow.traversalBefore));
        }
        return areEqual;
    }

    @Implementation
    @Override
    public int hashCode() {
        // This is 0 for a reason. If you change it, you will break the obtained
        // instances map in a manner that is remarkably difficult to debug.
        // Having a dynamic hash code keeps this object from being located
        // in the map if it was mutated after being obtained.
        return 0;
    }

    /**
     * Add a child node to this one. Also initializes the parent field of the
     * child.
     *
     * @param child The node to be added as a child.
     */
    public void addChild(AccessibilityNodeInfo child) {
        if (children == null) {
            children = new LinkedList<>();
        }

        children.add(child);
        ((ShadowAccessibilityNodeInfo) ShadowExtractor.extract(child)).parent =
                realAccessibilityNodeInfo;
    }

    @Implementation
    public void addChild(View child) {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain(child);
        addChild(node);
    }

    @Implementation
    public void addChild(View root, int virtualDescendantId) {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain(root, virtualDescendantId);
        addChild(node);
    }

    /**
     * @return The list of arguments for the various calls to performAction. Unmodifiable.
     */
    public List<Integer> getPerformedActions() {
        if (performedActionAndArgsList == null) {
            performedActionAndArgsList = new LinkedList<>();
        }

        // Here we take the actions out of the pairs and stick them into a separate LinkedList to return
        List<Integer> actionsOnly = new LinkedList<Integer>();
        Iterator<Pair<Integer, Bundle>> iter = performedActionAndArgsList.iterator();
        while (iter.hasNext()) {
            actionsOnly.add(iter.next().first);
        }

        return Collections.unmodifiableList(actionsOnly);
    }

    /**
     * @return The list of arguments for the various calls to performAction. Unmodifiable.
     */
    public List<Pair<Integer, Bundle>> getPerformedActionsWithArgs() {
        if (performedActionAndArgsList == null) {
            performedActionAndArgsList = new LinkedList<>();
        }
        return Collections.unmodifiableList(performedActionAndArgsList);
    }

    /**
     * @return A shallow copy.
     */
    private AccessibilityNodeInfo getClone() {
        // We explicitly avoid allocating the AccessibilityNodeInfo from the actual pool by using
        // the private constructor. Not doing so affects test suites which use both shadow and
        // non-shadow objects.
        final AccessibilityNodeInfo newInfo =
                ReflectionHelpers.callConstructor(AccessibilityNodeInfo.class);
        final ShadowAccessibilityNodeInfo newShadow =
                (ShadowAccessibilityNodeInfo) ShadowExtractor.extract(newInfo);

        newShadow.boundsInScreen = new Rect(boundsInScreen);
        newShadow.propertyFlags = propertyFlags;
        newShadow.contentDescription = contentDescription;
        newShadow.text = text;
        newShadow.performedActionAndArgsList = performedActionAndArgsList;
        newShadow.parent = parent;
        newShadow.className = className;
        newShadow.labeledBy = labeledBy;
        newShadow.view = view;
        newShadow.textSelectionStart = textSelectionStart;
        newShadow.textSelectionEnd = textSelectionEnd;
        newShadow.actionListener = actionListener;
            if (actionsArray != null) {
                newShadow.actionsArray = new ArrayList<>();
                newShadow.actionsArray.addAll(actionsArray);
            } else {
                newShadow.actionsArray = null;
            }

        if (children != null) {
            newShadow.children = new LinkedList<>();
            newShadow.children.addAll(children);
        } else {
            newShadow.children = null;
        }

        newShadow.refreshReturnValue = refreshReturnValue;
        newShadow.movementGranularities = movementGranularities;
        newShadow.packageName = packageName;
            newShadow.viewIdResourceName = viewIdResourceName;
            newShadow.collectionInfo = collectionInfo;
        newShadow.collectionItemInfo = collectionItemInfo;
        newShadow.inputType = inputType;
        newShadow.liveRegion = liveRegion;
        newShadow.rangeInfo = rangeInfo;
            newShadow.maxTextLength = maxTextLength;
        newShadow.error = error;
        newShadow.traversalAfter = null;
        if (traversalAfter != null) {
            newShadow.traversalAfter = obtain(traversalAfter);
        }
        newShadow.traversalBefore = null;
        if (traversalBefore != null) {
            newShadow.traversalBefore = obtain(traversalBefore);
        }
        return newInfo;
    }

    /**
     * Private class to keep different nodes referring to the same view straight
     * in the mObtainedInstances map.
     */
    private static class StrictEqualityNodeWrapper {
        public final AccessibilityNodeInfo mInfo;

        public StrictEqualityNodeWrapper(AccessibilityNodeInfo info) {
            mInfo = info;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }

            final StrictEqualityNodeWrapper wrapper = (StrictEqualityNodeWrapper) object;
            return mInfo == wrapper.mInfo;
        }

        @Override
        public int hashCode() {
            return mInfo.hashCode();
        }
    }

    /**
     * Shadow of AccessibilityAction.
     */
    @Implements(AccessibilityNodeInfo.AccessibilityAction.class)
    public static final class ShadowAccessibilityAction {
        private int id;
        private CharSequence label;

        public void __constructor__(int id, CharSequence label) {
            if (((id & (int)ReflectionHelpers.getStaticField(AccessibilityNodeInfo.class, "ACTION_TYPE_MASK")) == 0) && Integer.bitCount(id) != 1) {
                throw new IllegalArgumentException("Invalid standard action id");
            }
            this.id = id;
            this.label = label;
        }

        @Implementation
        public int getId() {
            return id;
        }

        @Implementation
        public CharSequence getLabel() {
            return label;
        }

        @Override
        @Implementation
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }

            if (other == this) {
                return true;
            }

            if (other.getClass() != AccessibilityAction.class) {
                return false;
            }

            return id == ((AccessibilityAction) other).getId();
        }

        @Override
        public String toString() {
            String actionSybolicName = ReflectionHelpers.callStaticMethod(
                    AccessibilityNodeInfo.class, "getActionSymbolicName", ClassParameter.from(int.class, id));
            return "AccessibilityAction: " + actionSybolicName + " - " + label;
        }
    }

    @Implementation
    public int describeContents() {
        return 0;
    }

    @Implementation
    public void writeToParcel(Parcel dest, int flags) {
        StrictEqualityNodeWrapper wrapper = new StrictEqualityNodeWrapper(realAccessibilityNodeInfo);
        int keyOfWrapper = -1;
        for (int i = 0; i < orderedInstances.size(); i++) {
            if (orderedInstances.valueAt(i).equals(wrapper)) {
                keyOfWrapper = orderedInstances.keyAt(i);
                break;
            }
        }
        dest.writeInt(keyOfWrapper);
    }

    private static int getActionTypeMaskFromFramework() {
        // Get the mask to determine whether an int is a legit ID for an action, defined by Android
        return (int)ReflectionHelpers.getStaticField(AccessibilityNodeInfo.class, "ACTION_TYPE_MASK");
    }

    private static AccessibilityAction getActionFromIdFromFrameWork(int id) {
        // Convert an action ID to Android standard Accessibility Action defined by Android
        return ReflectionHelpers.callStaticMethod(
                AccessibilityNodeInfo.class, "getActionSingleton", ClassParameter.from(int.class, id));
    }

    private static int getLastLegacyActionFromFrameWork() {
        return (int)ReflectionHelpers.getStaticField(AccessibilityNodeInfo.class, "LAST_LEGACY_STANDARD_ACTION");
    }
    /**
     * Configure the return result of an action if it is performed
     */
    public void setOnPerformActionListener(OnPerformActionListener listener) {
        actionListener = listener;
    }

    public interface OnPerformActionListener {
        boolean onPerformAccessibilityAction(int action, Bundle arguments);
    }
}