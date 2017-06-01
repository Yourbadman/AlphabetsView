package com.rocf.library;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * This widget contains: alphabet and pop txt,
 * <li>alphabet: support wave when slide it</>
 * <li>pop txt: when slide quickly, between pop txt has fade in and fade out animation</li>
 * <li>it support on mutil-screen or at RTL,alphabet txt size is changed if window height change</li>
 * <p>
 * it can set txt or drawable at alphabet and pop content by {@link AlphabetsView#setAlphabetList},
 * <p>
 * if you want to make it wave,set true {@link AlphabetsView#setIsAlphabetAnima} and the num of alphabet
 * is greater than default num(is 10,can set by users {@link AlphabetsView#setAlphabetPopLimiteNum(int)}).
 * <p>
 *
 * @author rocf.wong@gmail.com
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AlphabetsView extends LinearLayout implements View.OnLayoutChangeListener {

    private static final String TAG = "AlphabetsView";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    /**
     * Duration of fade-out animation.
     */
    private static final int DURATION_FADE_OUT = 300;

    /**
     * Duration of fade-in animation.
     */
    private static final int DURATION_FADE_IN = 150;

    /**
     * Duration of transition cross-fade animation.
     */
    private static final int DURATION_CROSS_FADE = 50;

    /**
     * Inactivity timeout before fading controls.
     */
    private static final long FADE_TIMEOUT = 1500;

    /**
     * Scroll thumb and preview not showing.
     */
    private static final int STATE_NONE = 0;

    /**
     * Scroll thumb visible and moving along with the scrollbar.
     */
    private static final int STATE_VISIBLE = 1;

    /**
     * Scroll thumb and preview being dragged by user.
     */
    private static final int STATE_CHANGE_TEXT = 2;

    private static int mPopNumLimit = Integer.MAX_VALUE;

    /**
     * The offset of touchRect's left.(dip)
     */
    private static final int TOUCH_RECT_LEFT_OFFSET_DIP = 25;

    private int mTouchRectLeftOffset;
    private int mDefaultToastTxtColor = Color.WHITE;
    private List<Alphabet> mAlphabetList;
    private HashMap<Integer, ValueAnimator> mAnimMap;
    private OnAlphabetListener mAlphabetListener;
    private float mSideIndexX;
    private float mSideIndexY;
    private int mSideIndexHeight;
    private int mIndexListSize;
    private int mViewWidth;
    private int mToastOffset;
    private int mToastTextSize;
    private int mAlphabetTextSize;
    private int mPaddingTopBottom;
    private int mAlphabetMaxOffset;
    private int mAlphabetLeftMargin, mAlphabetRightMargin;

    private int mMaxOffset;
    private int mMoveCount;
    private long mPopAnimTime;
    private long mBackAnimTime = 0;

    private float mLastFocusX;
    private float mLastFocusY;

    private Handler mHandler;
    private boolean mInSelect;
    private final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private final int LONG_SELECT = 0;

    private Drawable mToastBg;
    /**
     * Defines the selectedBg's location and dimension at drawing time
     */
    private Rect mSelectedRect = new Rect();

    private ViewGroupOverlay mOverlay;
    private final TextView mPrimaryText;
    private final TextView mSecondaryText;
    private final ImageView mPreviewImage;

    private final Rect mContainerRect = new Rect();
    private final Rect mTempBounds = new Rect();
    private final Rect mTempMargins = new Rect();
    private final Rect mTempRect = new Rect();

    /**
     * Set containing preview text transition animations.
     */
    private AnimatorSet mPreviewAnimation;
    /**
     * Set containing decoration transition animations.
     */
    private AnimatorSet mDecorAnimation;

    /**
     * Whether this view is currently performing layout.
     */
    private boolean mUpdatingLayout;

    /**
     * Whether the primary text is showing.
     */
    private transient boolean mShowingPrimary;

    /**
     * Whether the preview image is visible.
     */
    private boolean mShowingPreview;

    private int mPreSelection = -1;
    /**
     * The index of the current section.
     */
    private int mCurrentSelection = -1;
    /**
     * Padding in pixels around the preview text. Applied as layout margins to
     * the preview text and padding to the preview image.
     */
    //private final int mPreviewPadding;

    private boolean isShowSelected;
    private boolean isPoped = false;
    boolean isSetList = false;

    /* the slidebar selected color, default is blue */
    int mSelectedColor;

    int mDefaultColor = Color.BLACK;

    /**
     * Whether clip Alphabet, show initial.
     */
    private boolean mIsClipInitial = true;

    int mDefaultToastBgAlpha = 255;//255*0.9
    private ColorStateList mSelectedColorStateList;
    private Map<String, Drawable> mAlphabetDrawable = new HashMap<>();
    private Map<String, Drawable> mPopupDrawable = new HashMap<>();
    private boolean isRtl;
    private int mVisiableStart;
    private int mVisiableEnd;
    private boolean mInChooseable;
    //when select new alphabet ,the last selected will be set false
    private volatile boolean isSwitchLastSelected = true;
    private long mCurrentTouchTime = 0;
    private boolean mForceSelected;


    public AlphabetsView(Context context) {
        this(context, null);
    }

    public AlphabetsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("UseSparseArrays")
    public AlphabetsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources res = context.getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AlphabetsView, defStyle, 0);
        mMaxOffset = a.getDimensionPixelSize(R.styleable.AlphabetsView_maxOffset, 54);
        mMoveCount = a.getInteger(R.styleable.AlphabetsView_moveCount, 7);
        mPopAnimTime = a.getInteger(R.styleable.AlphabetsView_popAnimTime, 120);
        mToastBg = a.getDrawable(R.styleable.AlphabetsView_alphabetToastBg);
        mToastOffset = a.getDimensionPixelSize(R.styleable.AlphabetsView_toastOffset,
                res.getDimensionPixelSize(R.dimen.alphabets_view_toast_offset));
        mToastTextSize = a.getDimensionPixelSize(R.styleable.AlphabetsView_toastTextSize,
                res.getDimensionPixelSize(R.dimen.alphabets_view_toast_text_size));
        mAlphabetTextSize = a.getDimensionPixelSize(R.styleable.AlphabetsView_alphabetTextSize,
                res.getDimensionPixelSize(R.dimen.alphabets_view_alphabet_text_size));
        mAlphabetMaxOffset = a.getDimensionPixelSize(R.styleable.AlphabetsView_alphabetMaxOffset,
                res.getDimensionPixelSize(R.dimen.alphabets_view_alphabet_max_offset));
        mPaddingTopBottom = a.getDimensionPixelSize(R.styleable.AlphabetsView_paddingTopBottom,
                res.getDimensionPixelSize(R.dimen.alphabets_view_padding_top_bottom));
        mAlphabetLeftMargin = a.getDimensionPixelSize(R.styleable.AlphabetsView_alphabetLeftMargin,
                res.getDimensionPixelSize(R.dimen.alphabets_view_alphabet_left_margin));
        mAlphabetRightMargin = a.getDimensionPixelSize(R.styleable.AlphabetsView_alphabetRightMargin,
                res.getDimensionPixelSize(R.dimen.alphabets_view_alphabet_right_margin));
        mSelectedColorStateList = a.getColorStateList(R.styleable.AlphabetsView_sectedColors);
        a.recycle();
        mSelectedColor = res.getColor(R.color.alphabets_view_selected_color);
        mDefaultColor = res.getColor(R.color.alphabets_view_default_color);
        if (mToastBg == null) {
            mToastBg = res.getDrawable(R.drawable.alphabets_view_toast_bg);
            mToastBg.setAlpha(mDefaultToastBgAlpha);
        }
        if (mSelectedColorStateList == null) {
            mSelectedColorStateList = res.getColorStateList(R.color.alphabets_view_text_color);
        }

        mDefaultToastTxtColor = res.getColor(R.color.alphabets_view_toast_txt_color);
        mViewWidth = res.getDimensionPixelSize(R.dimen.alphabets_view_width);
        mTouchRectLeftOffset = dip2px(TOUCH_RECT_LEFT_OFFSET_DIP);
        mHandler = new GestureHandler();
        mAlphabetList = new ArrayList<Alphabet>();
        mAnimMap = new HashMap<Integer, ValueAnimator>();
        isShowSelected = true;
        mPreviewImage = new ImageView(context);
        mPreviewImage.setMinimumWidth(mToastBg.getIntrinsicWidth());
        mPreviewImage.setMinimumHeight(mToastBg.getIntrinsicHeight());
        mPreviewImage.setBackground(mToastBg);
        mPreviewImage.setAlpha(0f);

        final int textMinSize = Math.max(0, mToastTextSize);
        mPrimaryText = createPreviewTextView();
        mPrimaryText.setMinimumWidth(textMinSize);
        mPrimaryText.setMinimumHeight(textMinSize);
        mSecondaryText = createPreviewTextView();
        mSecondaryText.setMinimumWidth(textMinSize);
        mSecondaryText.setMinimumHeight(textMinSize);
        isCenter();

        this.addOnLayoutChangeListener(this);

    }

    private static final Interpolator mInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    @SuppressLint("HandlerLeak")
    private class GestureHandler extends Handler {
        GestureHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LONG_SELECT:
                    mInSelect = true;
                    popAlphabet();
                    break;
                default:
                    throw new RuntimeException("Unknown message " + msg); //never
            }
        }
    }

    private class Alphabet {
        String firstAlphabet;
        //int position; // unused
        boolean isBackgoundDrawable;

    }

    public static interface OnAlphabetListener {
        /**
         * When alphabet is changed
         *
         * @param alphabetPosition position in List
         * @param firstAlphabet    the first alphabet
         */
        void onAlphabetChanged(int alphabetPosition, String firstAlphabet);
    }

    /**
     * set alphabet listener
     *
     * @param aListener
     */
    public void setOnAlphabetListener(OnAlphabetListener aListener) {
        mAlphabetListener = aListener;
    }

    private void isCenter() {
        setGravity(Gravity.CENTER);
        setPaddingRelative(27, mPaddingTopBottom, 0, mPaddingTopBottom);
    }

    /**
     * set the begin of alphabet list.
     *
     * @param alphabetBackgoundDrawable the show drawable in alphabet
     */
    public void setAlphabetList(List<String> listStr, Map<String, Drawable> alphabetBackgoundDrawable, Map<String, Drawable> popupBackgoundDrawable) {
        this.mPopupDrawable = popupBackgoundDrawable;
        this.mAlphabetDrawable = alphabetBackgoundDrawable;
        setAlphabetList(listStr);
    }

    /**
     * The incoming string List, clip each initials, to be displayed.
     *
     * @param listStr the list contains initial alphabet.
     */
    public void setAlphabetList(List<String> listStr) {
        if (DEBUG) {
            Log.d(TAG, "setAlphabetList-->");
        }
        mAlphabetList.clear();

        List<String> countries = listStr;
        // Don't sort inside.
        //Collections.sort(countries);

        String previousAlphabet = null;
        Pattern numberPattern = Pattern.compile("[0-9]");

        for (String country : countries) {
            String firstAlphabet = mIsClipInitial ? country.substring(0, 1) : country;

            // Group numbers together in the scroller
            if (mIsClipInitial && numberPattern.matcher(firstAlphabet).matches()) {
                firstAlphabet = "#";
            }

            // If we've changed to a new Alphabet, add the previous Alphabet to the alphabet scroller
            if (previousAlphabet != null && !firstAlphabet.equals(previousAlphabet)) {
                String tempLeter = mIsClipInitial ? previousAlphabet.toUpperCase(Locale.UK) : previousAlphabet;
                Alphabet alphabet = new Alphabet();
                alphabet.firstAlphabet = tempLeter;
                mAlphabetList.add(alphabet);
                //alphabet.position = mAlphabetList.size() - 1;
            }

            previousAlphabet = firstAlphabet;
        }

        if (previousAlphabet != null) {
            // Save the last Alphabet
            Alphabet alphabet = new Alphabet();
            alphabet.firstAlphabet = previousAlphabet;
            mAlphabetList.add(alphabet);
            //alphabet.position = mAlphabetList.size() - 1;
        }

        isSetList = true;
        requestLayout();
        if (DEBUG) {
            Log.d(TAG, "<--setAlphabetList");
        }
    }

    /**
     * Removes this FastScroller overlay from the host view.
     */
    public void remove() {
        mOverlay.remove(mPreviewImage);
        mOverlay.remove(mPrimaryText);
        mOverlay.remove(mSecondaryText);
    }

    /**
     * Measures and layouts the scrollbar and decorations.
     */
    public void updatePopAlphabetLayout() {
        // Prevent re-entry when RTL properties change as a side-effect of
        // resolving padding.
        if (mUpdatingLayout) {
            return;
        }

        mUpdatingLayout = true;

        updateContainerRect();

        final Rect bounds = mTempBounds;
        int textPadding = 0;
        measurePreview(mPrimaryText, bounds);
        textPadding = (mPrimaryText.getMeasuredHeight() - mPrimaryText.getMinimumHeight()) / 2;
        bounds.top -= textPadding;
        bounds.bottom += textPadding;
        applyLayout(mPrimaryText, bounds);

        measurePreview(mSecondaryText, bounds);
        textPadding = (mSecondaryText.getMeasuredHeight() - mSecondaryText.getMinimumHeight()) / 2;
        bounds.top -= textPadding;
        bounds.bottom += textPadding;
        applyLayout(mSecondaryText, bounds);

        if (mPreviewImage != null) {
            measurePreview(mPreviewImage, bounds);
            applyLayout(mPreviewImage, bounds);
        }
    }

    /**
     * setSelection, if user is touching on the AWView, the function is invalid.
     *
     * @param index
     */
    public void setSelection(int index) {
        if (DEBUG) {
            Log.d(TAG, "setSelection{"
                    + " currentIndex:" + index
                    + " startIndex:" + mVisiableStart
                    + " endIndex:" + mVisiableEnd + " }");
        }
        if (index >= 0 && index < mAlphabetList.size()) {
            View textV = getChildAt(index);
            View curTextV = getChildAt(mCurrentSelection);
            if (curTextV != null && isSwitchLastSelected) {
                curTextV.setSelected(false);
            }
            mCurrentSelection = index;
            if (textV != null) {
                if (isPoped) {
                    textV.setSelected(false);
                } else {
                    textV.setSelected(true);
                }
            }
            if (!isPoped) {
                isShowSelected = true;
                invalidate();
            }

            if (mShowingPreview) {
                setState(STATE_CHANGE_TEXT);
                setState(STATE_NONE);
            }
        }
    }

    /**
     * setSelection, if user is touching on the AWView, the function is invalid.
     *
     * @param start the highlight alphabet start position
     * @param end   the highlight alphabet end position
     */
    public void setSelection(int start, int end, int currentIndex) {
        //UnVisiable
        for (int i = mVisiableStart; i <= mVisiableEnd; i++) {
            View textV = getChildAt(i);
            if (textV != null) {
                textV.setSelected(false);
            }
        }
        //Visiable
        for (int i = start; i <= end; i++) {
            View textV = getChildAt(i);
            if (textV != null) {
                textV.setSelected(true);
            }
        }
        mVisiableStart = start;
        mVisiableEnd = end;
        if (start <= mCurrentSelection || mCurrentSelection <= end)
            isSwitchLastSelected = false;
        setSelection(currentIndex);
    }

    /**
     * Set toast color.
     *
     * @param argb The color used to fill the shape
     */
    public void setToastBackGroundColor(int argb) {
        if (mToastBg instanceof GradientDrawable) {
            GradientDrawable gDrawable = (GradientDrawable) mToastBg;
            gDrawable.setColor(argb);
        }
    }

    /**
     * Set toast color.
     *
     * @param argb The color used to fill the shape
     * @param argb The alpha of bg
     */
    public void setToastBackGroundColor(int argb, int alpha) {
        if (mToastBg instanceof GradientDrawable) {
            GradientDrawable gDrawable = (GradientDrawable) mToastBg;
            gDrawable.setAlpha(alpha);
            gDrawable.setColor(argb);
        }
    }


    /**
     * Set selected color.
     */
    public void setSelectedBackGroundColor(int argb) {
        if (argb != mSelectedColor) {
            mSelectedColor = argb;
            mSelectedColorStateList = new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},
                    new int[]{}},
                    new int[]{mSelectedColor, mDefaultColor});
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(mSelectedColorStateList);
                }
            }
        }
    }

    /**
     * Set text default color.
     */
    public void setTextDefaultColor(int argb) {
        if (argb != mDefaultColor) {
            mDefaultColor = argb;
            mSelectedColorStateList = new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},
                    new int[]{}},
                    new int[]{mSelectedColor, mDefaultColor});
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(mSelectedColorStateList);
                }
            }
        }
    }

    /**
     * setIsClipInitial
     *
     * @param isClipInitial clip is true, no clip is false.R
     */
    public void setIsClipInitial(boolean isClipInitial) {
        mIsClipInitial = isClipInitial;
    }


    /**
     * set the min number of alphabet what alphabet do animation
     * when touch.the default limit num is 10.
     * <p>
     * Means:
     * if don't want to do alphabet animation,can set limit num that
     * more than the alphabet's num.
     *
     * @param mPopNumLimit the num of limit.
     */
    public void setAlphabetPopLimiteNum(int mPopNumLimit) {
        this.mPopNumLimit = mPopNumLimit;
    }

    /**
     * @param isAnima
     */
    public void setIsAlphabetAnima(boolean isAnima) {
        if (isAnima) {
            this.mPopNumLimit = 0;
        } else {
            this.mPopNumLimit = Integer.MAX_VALUE;
        }
    }

    /**
     * @return isClipIntial
     */
    public boolean isClipIntial() {
        return mIsClipInitial;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean pointerUp = (event.getAction() & MotionEvent.ACTION_MASK)
                == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        float sumX = 0, sumY = 0;
        final int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += event.getX(i);
            sumY += event.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float focusX = sumX / div;
        final float focusY = sumY / div;
        getLocalVisibleRect(mTempRect);
        // adjust touchRect, add left offset.
        int tempTouchRectLeftOffset = isRtl ? -mTouchRectLeftOffset : mTouchRectLeftOffset;
        mTempRect.left = mTempRect.left + tempTouchRectLeftOffset;
        if (focusY < getPaddingTop() || focusY > getHeight() - getPaddingBottom()
                || !mTempRect.contains((int) focusX, (int) focusY)) {
            if (DEBUG) {
                Log.d(TAG, "onTouch: outside!");
            }
            updateAlphabetTouch(false);
            isShowSelected = true;
            invalidate();
            mHandler.removeMessages(LONG_SELECT);
            backAlphabet(true);// back selected alphabet.
            mAnimMap.clear();
            setState(STATE_NONE);
            if (!isPoped) {
                isShowSelected = true;
                invalidate();
            }
            changeTextColor(mCurrentSelection, true);
            return false;
        }

        if (mCurrentSelection != mPreSelection && event.getAction() == MotionEvent.ACTION_MOVE) {
            changeTextColor(mCurrentSelection, false);
        }


        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mCurrentTouchTime = SystemClock.elapsedRealtime();
                mSideIndexX = mLastFocusX = focusX;
                mSideIndexY = mLastFocusY = focusY;
                mInSelect = false;
                updateAlphabetTouch(true);
                selectAlphabet();
                isShowSelected = false;
                int itemPosition = getItemPosition();
                if (mPreSelection == itemPosition) {
                    changeTextColor(mCurrentSelection, false);
                }
                invalidate();
                isPoped = false;
                if (mAlphabetList.size() >= mPopNumLimit) {
                    mHandler.removeMessages(LONG_SELECT);
                    mHandler.sendEmptyMessageAtTime(LONG_SELECT, event.getDownTime()
                            + TAP_TIMEOUT /*+ LONGPRESS_TIMEOUT*/);// same as LONGPRESS_TIMEOUT
                }
                break;

            case MotionEvent.ACTION_MOVE:
                updateAlphabetTouch(true);
                if ((SystemClock.elapsedRealtime() - mCurrentTouchTime) < (DURATION_CROSS_FADE + 30)) {
                    if (DEBUG) {
                        Log.d(TAG, "onTouch: move too quickly,ignore it !!!");
                    }
                    break;
                }
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;
                if (!mInSelect) {
                    if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                        mSideIndexX = mSideIndexX - scrollX;
                        mSideIndexY = mSideIndexY - scrollY;
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;
                        if (mSideIndexX >= 0 && mSideIndexY >= 0) {
                            isShowSelected = false;
                            invalidate();
                            selectAlphabet();
                        }
                        break;
                    }
                }
                if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    mSideIndexX = mSideIndexX - scrollX;
                    mSideIndexY = mSideIndexY - scrollY;
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                    isShowSelected = false;
                    invalidate();
                    if (mSideIndexX >= 0 && mSideIndexY >= 0) {
                        if (selectAlphabet()) {
                            popAlphabet();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateAlphabetTouch(false);
                if (mInSelect) {
                    mInSelect = false;
                }
                isShowSelected = true;
                invalidate();
                mHandler.removeMessages(LONG_SELECT);

                backAlphabet(true);// back selected alphabet.
                changeTextColor(mCurrentSelection, true);
                isPoped = false;// PS: mBackAnimTime = 0

                mAnimMap.clear();
                setState(STATE_NONE);
                break;
            default:
                break;
        }
        return true;
    }

    private void updateAlphabetTouch(boolean isEnable) {
        if (mInChooseable != isEnable) {
            mInChooseable = isEnable;
            for (int i = 0; i < getChildCount(); i++) {
                View alphabetChild = getChildAt(i);
                //avoid mutil set
                if (alphabetChild.isEnabled() == isEnable)
                    break;
                alphabetChild.setEnabled(isEnable);
            }
        }
    }

    /**
     * Pop alphabet
     */
    protected void popAlphabet() {
        if (mAlphabetList.size() < mPopNumLimit) {
            return;
        }
        backAlphabet(false);// first, back alphabet, and pop alphabet

        isPoped = true;
        changeTextColor(mCurrentSelection, false);
        isShowSelected = false;
        invalidate();

        int position;
        int halfMoveCount = (mMoveCount + 1) / 2;

        int tempMaxOffset = isRtl ? -mMaxOffset : mMaxOffset;
        for (int i = 0; i < mMoveCount; i++) {
            position = mCurrentSelection - halfMoveCount + 1 + i;
            if (position >= 0 && position < getChildCount()) {
                View view = getChildAt(position);
                ValueAnimator tmpAnimator = (view != null) ? ObjectAnimator.ofFloat(view, "translationX", view.getTranslationX(),
                        -tempMaxOffset * (float) Math.sin((i + 1) * Math.PI / (mMoveCount + 1))) : null;
                if (tmpAnimator == null)
                    return;
                tmpAnimator.setDuration(mPopAnimTime);
                tmpAnimator.setRepeatCount(0);
                tmpAnimator.setInterpolator(mInterpolator);
                tmpAnimator.start();
                mAnimMap.put(position, tmpAnimator);
            }
        }
    }

    /**
     * Back alphabet.
     *
     * @param isSelected 是否是选中的字母周围弹出的字母
     */
    protected void backAlphabet(boolean isSelected) {
        if (mAlphabetList.size() < mPopNumLimit) {
            return;
        }

        int halfMoveCount = (mMoveCount + 1) / 2;
        for (int i = 0; i < mAlphabetList.size(); i++) {
            ValueAnimator vaAnim = mAnimMap.get(i);
            if (vaAnim != null) {
                vaAnim.cancel();
            }
            // Back around the selected alphabet place.
            if (isSelected && i > mCurrentSelection - halfMoveCount && i < mCurrentSelection + halfMoveCount) {
                View view = getChildAt(i);

                float tX = (view != null) ? view.getTranslationX() : 0f;
                if (tX < 0f || tX > 0f) {
                    doBackAnim(view);
                }
                // Back the unselected alphabet place.
            } else if (i <= mCurrentSelection - halfMoveCount || i >= mCurrentSelection + halfMoveCount) {
                View view = getChildAt(i);

                float tX = (view != null) ? view.getTranslationX() : 0f;
                if (tX < 0f || tX > 0f) {
                    doBackAnim(view);
                }
            }
        }
    }

    private int getItemPosition() {
        mSideIndexHeight = getHeight() - 2 * getPaddingTop();// paddingTop is variational.see updateView()
        // compute number of pixels for every side index item
        double pixelPerIndexItem = (double) mSideIndexHeight / mIndexListSize;
        // compute the item index for given event position belongs to
        int itemPosition = (int) ((mSideIndexY - getPaddingTop()) / pixelPerIndexItem);
        return itemPosition;
    }

    protected boolean selectAlphabet() {
        final int itemPosition = getItemPosition();
        // get the item (we can do it since we know item index)
        if (itemPosition < mAlphabetList.size() && (mCurrentSelection != itemPosition || mForceSelected)) {
            mPreSelection = mCurrentSelection;
            String firstAlphabet = mAlphabetList.get(itemPosition).firstAlphabet;

            View textV = getChildAt(itemPosition);// current
            View curTextV = getChildAt(mCurrentSelection);// pre

            if (curTextV != null && textV != null) {
                curTextV.setSelected(false);
            }
            mCurrentSelection = itemPosition;

            if (textV != null) {
                textV.setSelected(true);
            }

            if (mShowingPreview) {
                setState(STATE_CHANGE_TEXT);
            } else {
                setState(STATE_VISIBLE);
            }

            // notify alphabet changed.
            if (mAlphabetListener != null) {
                mAlphabetListener.onAlphabetChanged(itemPosition, firstAlphabet);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ViewGroup viewGroup = (ViewGroup) getParent();
        mOverlay = viewGroup.getOverlay();
        mOverlay.add(mPreviewImage);
        mOverlay.add(mPrimaryText);
        mOverlay.add(mSecondaryText);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (isSetList) {
            adjustPadding();
            requestLayout();
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view != null) {
                    view.requestLayout();
                }
            }
            isPoped = false;
            isSetList = false;
        }
        mUpdatingLayout = false;
        updatePopAlphabetLayout();
        if (!isPoped) {
            changeTextColor(mCurrentSelection, true);
            if (!(mVisiableStart == 0 && mVisiableEnd == 0)) {
                for (int i = mVisiableStart; i <= mVisiableEnd; i++) {
                    changeTextColor(i, true);
                }
            }

        }
        isRtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? true : false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isSetList) {
            mIndexListSize = mAlphabetList.size();
            addTextView();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    private void setState(int state) {
        removeCallbacks(mDeferHide);
        if (DEBUG) {
            Log.d(TAG, "setState: :" + (state == STATE_NONE ? "STATE_NOE" : (state == STATE_VISIBLE ? "STATE_VISIBLE" : "STATE_CHANGE_TEXT")));
        }
        switch (state) {
            case STATE_NONE:
                postAutoHide();
                break;
            case STATE_VISIBLE:
                AlphabetsView.Alphabet tmpAlphabet = mAlphabetList.get(mCurrentSelection);
                String text = tmpAlphabet.firstAlphabet;
                int textPadding = 0;
                final Rect bounds = mTempBounds;
                final TextView showing;
                final TextView target;
                if (mShowingPrimary) {
                    showing = mPrimaryText;
                    target = mSecondaryText;
                } else {
                    showing = mSecondaryText;
                    target = mPrimaryText;
                }
                if (tmpAlphabet.isBackgoundDrawable) {
                    // Set and layout target immediately.
                    target.setText("");
                    showing.setText("");
                    showing.setBackground(null);
                    target.setBackground(mPopupDrawable.get(text));
                } else {
                    textPadding = (target.getMeasuredHeight() - target.getMinimumHeight()) / 2;
                    showing.setBackground(null);
                    target.setBackground(null);
                    showing.setText("");
                    // Set and layout target immediately.
                    target.setText(text);
                }
                measurePreview(target, bounds);
                bounds.top -= textPadding;
                bounds.bottom += textPadding;
                applyLayout(target, bounds);
                transitionToVisible();
                break;
            case STATE_CHANGE_TEXT:
                if (!transitionPreviewLayout(mCurrentSelection)) {
                    transitionToHidden();
                }
                break;
        }
    }

    /**
     * Used to delay hiding fast scroll decorations.
     */
    private final Runnable mDeferHide = new Runnable() {
        @Override
        public void run() {
            transitionToHidden();
        }
    };

    /**
     * Constructs an animator for the specified property on a group of views.
     * See {@link ObjectAnimator#ofFloat(Object, String, float...)} for
     * implementation details.
     *
     * @param property The property being animated.
     * @param value    The value to which that property should animate.
     * @param views    The target views to animate.
     * @return An animator for all the specified views.
     */
    private static Animator groupAnimatorOfFloat(
            Property<View, Float> property, float value, View... views) {
        AnimatorSet animSet = new AnimatorSet();
        AnimatorSet.Builder builder = null;

        for (int i = views.length - 1; i >= 0; i--) {
            final Animator anim = ObjectAnimator.ofFloat(views[i], property, value);
            if (builder == null) {
                builder = animSet.play(anim);
            } else {
                builder.with(anim);
            }
        }

        return animSet;
    }

    /**
     * Shows nothing.
     */
    private void transitionToHidden() {
        if (DEBUG) {
            Log.d(TAG, "transitionToHidden# "
                    + " mPrimaryText:" + mPrimaryText.getText()
                    + " mSecondaryText:" + mSecondaryText.getText());
        }
        //create it at postAutoHide() to preMake.
        mDecorAnimation.start();
        mShowingPreview = false;
    }

    private void makeHideAnimations() {
        final Animator fadeOut = groupAnimatorOfFloat(View.ALPHA, 0f,
                mPreviewImage, mSecondaryText, mPrimaryText).setDuration(DURATION_FADE_OUT);
        mDecorAnimation = new AnimatorSet();
        mDecorAnimation.playTogether(fadeOut);
    }

    /**
     * Shows the toast.
     */
    private void transitionToVisible() {

        if (DEBUG) {
            Log.d(TAG, "transitionToVisible# "
                    + " mPrimaryText:" + mPrimaryText.getText()
                    + " mSecondaryText:" + mSecondaryText.getText());
        }
        if (mDecorAnimation != null) {
            mDecorAnimation.cancel();
        }
        final Animator fadeIn = groupAnimatorOfFloat(View.ALPHA, 1f,
                mPreviewImage, mSecondaryText, mPrimaryText).setDuration(DURATION_FADE_IN);
        mDecorAnimation = new AnimatorSet();
        mDecorAnimation.playTogether(fadeIn);
        mDecorAnimation.start();
        mShowingPreview = true;
    }

    private void postAutoHide() {
        removeCallbacks(mDeferHide);
        makeHideAnimations();
        postDelayed(mDeferHide, FADE_TIMEOUT);
    }

    private void addTextView() {
        removeAllViews();
        if (mIndexListSize < 1) {
            return;
        }
        int beginTextPos = 1;

        int alphabetsHeight = getMeasuredHeight() - 2 * getFlexiblePadding();
        int currrentMaxTextSize = alphabetsHeight / mIndexListSize;
        if (DEBUG) {
            Log.d(TAG, "getMeasuredHeight: " + getMeasuredHeight()
                    + " getFlexiblePadding: " + getFlexiblePadding()
                    + " alphabetsHeight: " + alphabetsHeight
                    + " mIndexListSize: " + mIndexListSize
                    + " currrentMaxTextSize:" + currrentMaxTextSize
                    + " Default AlphabetTextSize:" + mAlphabetTextSize);
        }
        //bg drawable can't set width/height,so must set padding to compat.
        if (mAlphabetTextSize < currrentMaxTextSize || currrentMaxTextSize < 0) {
            currrentMaxTextSize = mAlphabetTextSize;
        }

        AlphabetsView.Alphabet tmpAlphabet = null;
        for (double i = beginTextPos; i <= mIndexListSize; i++) {
            tmpAlphabet = mAlphabetList.get((int) i - 1);
            String tmpAlphabetKey = tmpAlphabet.firstAlphabet;
            tmpAlphabet.isBackgoundDrawable = (mAlphabetDrawable.containsKey(tmpAlphabetKey) && mAlphabetDrawable.get(tmpAlphabetKey) != null) ? true : false;
            LayoutParams params = null;
            //add drawable at alphabet list.
            if (tmpAlphabet.isBackgoundDrawable) {
                ImageView tmpIv = new ImageView(getContext());
                tmpIv.setImageDrawable(mAlphabetDrawable.get(tmpAlphabetKey));
                params = new LayoutParams(
                        currrentMaxTextSize, currrentMaxTextSize, 1);
                params.setMarginStart(mAlphabetLeftMargin);
                params.setMarginEnd(mAlphabetRightMargin);
                tmpIv.setLayoutParams(params);
                tmpIv.setEnabled(false);
                addView(tmpIv);
                tmpIv.requestLayout();
            } else { //add txt at alphabet list.
                TextView tmpTV = new TextView(getContext());
                tmpTV.setTextColor(mSelectedColorStateList);
                params = new LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                tmpTV.setText(tmpAlphabetKey);
                tmpTV.setGravity(Gravity.CENTER);
                tmpTV.setTextSize(TypedValue.COMPLEX_UNIT_PX, currrentMaxTextSize);
                params.setMarginStart(mAlphabetLeftMargin);
                params.setMarginEnd(mAlphabetRightMargin);
                tmpTV.setTypeface(Typeface.create("helve-neue-regular", Typeface.NORMAL));
                tmpTV.setLayoutParams(params);
                tmpTV.setIncludeFontPadding(false);
                tmpTV.setEnabled(false);
                addView(tmpTV);
                tmpTV.requestLayout();
            }

        }
    }

    private void adjustPadding() {
        if (mIndexListSize == 0) {
            return;
        }
        int padding = getFlexiblePadding();
        if (DEBUG) {
            Log.d(TAG, " #adjustPadding" +
                    " padding:" + padding
                    + " Default PaddingTopBottom:" + mPaddingTopBottom
                    + " getHeight:" + getHeight()
                    + " default max alphabetOffset:" + mAlphabetMaxOffset
                    + " current indexList size " + mIndexListSize
                    + " default alphabetTxt size:" + mAlphabetTextSize
                    + " alphabet size:" + mAlphabetList.size());
        }
        if (padding > mPaddingTopBottom) {
            setPadding(getPaddingStart(), padding, getPaddingEnd(), padding);
        } else {
            setPadding(getPaddingStart(), mPaddingTopBottom, getPaddingEnd(), mPaddingTopBottom);
        }
    }

    private int getFlexiblePadding() {
        int padding = (getHeight() - mAlphabetMaxOffset * (mIndexListSize - 1) - mAlphabetTextSize * mIndexListSize) / 2;
        if (padding > mPaddingTopBottom) {
            return padding;
        } else {
            return mPaddingTopBottom;
        }
    }

    /**
     * Transitions the preview text to a new section. Handles animation,
     * measurement, and layout. If the new preview text is empty, returns false.
     *
     * @param sectionIndex The section index to which the preview should
     *                     transition.
     * @return False if the new preview text is empty.
     */
    private boolean transitionPreviewLayout(int sectionIndex) {

        AlphabetsView.Alphabet tmpAlphabet = mAlphabetList.get(sectionIndex);
        String text = tmpAlphabet.firstAlphabet;

        final Rect bounds = mTempBounds;
        final TextView showing;
        final TextView target;
        if (mShowingPrimary) {
            showing = mPrimaryText;
            target = mSecondaryText;
        } else {
            showing = mSecondaryText;
            target = mPrimaryText;
        }
        if (DEBUG) {
            Log.d(TAG, "transitionPreviewLayout# "
                    + " sectionIndex" + sectionIndex
                    + " text:" + text
                    + " mShowingPrimary:" + mShowingPrimary);
        }
        int textPadding = 0;
        if (tmpAlphabet.isBackgoundDrawable) {
            // Set and layout target immediately.
            target.setText("");
            target.setBackground(mPopupDrawable.get(text));
        } else {
            textPadding = (target.getMeasuredHeight() - target.getMinimumHeight()) / 2;
            target.setBackground(null);
            // Set and layout target immediately.
            target.setText(text);
        }
        measurePreview(target, bounds);
        bounds.top -= textPadding;
        bounds.bottom += textPadding;
        applyLayout(target, bounds);

        if (mPreviewAnimation != null) {
            mPreviewAnimation.cancel();
        }
        // Cross-fade preview text.
        final Animator showTarget = animateAlpha(target, 1f).setDuration(DURATION_CROSS_FADE);
        final Animator hideShowing = animateAlpha(showing, 0f).setDuration(DURATION_CROSS_FADE);
        hideShowing.addListener(mSwitchPrimaryListener);

        // Apply preview image padding and animate bounds, if necessary.
        bounds.left -= mPreviewImage.getPaddingLeft();
        bounds.top -= mPreviewImage.getPaddingTop();
        bounds.right += mPreviewImage.getPaddingRight();
        bounds.bottom += mPreviewImage.getPaddingBottom();
        /*final Animator resizePreview = animateBounds(preview, bounds);
        resizePreview.setDuration(DURATION_RESIZE);*/
        mPreviewAnimation = new AnimatorSet();
        mPreviewAnimation.play(hideShowing).with(showTarget);
        mPreviewAnimation.start();

        return !TextUtils.isEmpty(text);
    }

    /**
     * Used to effect a transition from primary to secondary text.
     */
    private final AnimatorListener mSwitchPrimaryListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mShowingPrimary = !mShowingPrimary;
        }
    };

    /**
     * Returns an animator for the view's alpha value.
     */
    private static Animator animateAlpha(View v, float alpha) {
        return ObjectAnimator.ofFloat(v, View.ALPHA, alpha);
    }

    /**
     * Measures the preview text bounds, taking preview image padding into
     * account.
     *
     * @param v   The preview text view to measure.
     * @param out Rectangle into which measured bounds are placed.
     */
    private void measurePreview(View v, Rect out) {
        // Apply the preview image's padding as layout margins.
        final Rect margins = mTempMargins;
        margins.left = mPreviewImage.getPaddingLeft();
        margins.top = mPreviewImage.getPaddingTop();
        margins.right = mPreviewImage.getPaddingRight();
        margins.bottom = mPreviewImage.getPaddingBottom();

        measureFloating(v, margins, out);
    }

    private void measureFloating(View preview, Rect margins, Rect out) {
        final int marginLeft;
        final int marginTop;
        final int marginRight;
        if (margins == null) {
            marginLeft = 0;
            marginTop = 0;
            marginRight = 0;
        } else {
            marginLeft = margins.left;
            marginTop = margins.top;
            marginRight = margins.right;
        }

        final Rect container = mContainerRect;
        final int containerWidth = container.width();
        final int adjMaxWidth = containerWidth - marginLeft - marginRight;
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(adjMaxWidth, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        preview.measure(widthMeasureSpec, heightMeasureSpec);

        // Align at the vertical center, mToastOffset away from this View.
        final int containerHeight = container.height();
        final int width = preview.getMeasuredWidth();
        final int top = (containerHeight - preview.getMinimumHeight()) / 2 + container.top;
        final int bottom = top + preview.getMinimumHeight();
        //final int left = containerWidth - mViewWidth - mToastOffset - width + container.left;
        final int left = (getResources().getDisplayMetrics().widthPixels - width) / 2;
        final int right = left + width;
        out.set(left, top, right, bottom);
    }

    /**
     * Creates a view into which preview text can be placed.
     */
    private TextView createPreviewTextView() {
        final LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        final TextView textView = new TextView(getContext());
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
        textView.setTextColor(mDefaultToastTxtColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mToastTextSize);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.CENTER);
        textView.setAlpha(0f);
        textView.setTypeface(Typeface.create("helve-neue-regular", Typeface.NORMAL));
        return textView;
    }

    /**
     * Updates the container rectangle used for layout.
     */
    private void updateContainerRect() {
        final Rect container = mContainerRect;
        container.left = getLeft() + getPaddingLeft();
        container.top = getTop() + getPaddingTop();
        container.right = getRight() - getPaddingRight();
        container.bottom = getBottom() - getPaddingBottom();
    }

    /**
     * Layouts a view within the specified bounds and pins the pivot point to
     * the appropriate edge.
     *
     * @param view   The view to layout.
     * @param bounds Bounds at which to layout the view.
     */
    private void applyLayout(View view, Rect bounds) {
        view.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
        view.setPivotX(bounds.right - bounds.left);
    }

    private void doBackAnim(final View view) {
        ValueAnimator tmpAnimator = ObjectAnimator.ofFloat(view, "translationX", view.getTranslationX(), 0);
        tmpAnimator.setDuration(mBackAnimTime);
        tmpAnimator.setRepeatCount(0);
        tmpAnimator.start();
    }

    private void changeTextColor(int position, boolean isWhite) {
        View curTextV = getChildAt(position);
        if (curTextV != null) {
            curTextV.setSelected(isWhite);
        }
    }

    private int dip2px(float dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (oldBottom != 0 && oldBottom != bottom) {
            if (DEBUG) {
                Log.d(TAG, " onLayoutChange:-> oldBottom:" + oldBottom + " bottom:" + bottom);
            }
            adjustPadding();
            addTextView();
        }
    }

    /**
     * Set the toast text color.
     *
     * @param color
     */
    public void setToastTextColor(int color) {
        this.mDefaultToastTxtColor = color;
    }

    /**
     * Get alphabet current selection.
     *
     * @return
     */
    public int getCurrentSelectedPos() {

        return this.mCurrentSelection;
    }

    /**
     * Get AlphabetList
     *
     * @return AlphabetList
     */
    public List<String> getAlphabetList() {
        List<String> mList = new ArrayList<>();
        for (Alphabet alphabet : mAlphabetList) {
            mList.add(alphabet.firstAlphabet);
        }
        return mList;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.currentIndex = mCurrentSelection;
        myState.startIndex = mVisiableStart;
        myState.endIndex = mVisiableEnd;
        if (DEBUG) {
            Log.d(TAG, " onSaveInstanceState:->" + myState);
        }
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState != null) {
            setSelection(myState.startIndex, myState.endIndex, myState.currentIndex);
            if (DEBUG) {
                Log.d(TAG, " onRestoreInstanceState:->" + myState);
            }
        }
    }

    private static class SavedState extends BaseSavedState {
        int currentIndex;
        int startIndex;
        int endIndex;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            currentIndex = source.readInt();
            startIndex = source.readInt();
            endIndex = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentIndex);
            dest.writeInt(startIndex);
            dest.writeInt(endIndex);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        @Override
        public String toString() {
            return "AlphabetWavesView.SavedState{"
                    + " currentIndex=" + currentIndex
                    + " startIndex=" + startIndex
                    + " endIndex=" + endIndex
                    + "}";
        }
    }

    /**
     * pop alphabet when click again in the same position.
     *
     * @param isRepeateClickOnAlphabets
     */
    public void setRepeatClickOnAlphabets(boolean isRepeateClickOnAlphabets) {
        this.mForceSelected = isRepeateClickOnAlphabets;
    }
}
