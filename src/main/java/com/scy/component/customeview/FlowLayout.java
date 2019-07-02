package com.scy.component.customeview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class FlowLayout extends ViewGroup {
    private int gravity;
    private ArrayList<View> lineViews;
    private List<List<View>> views;
    private List<Integer> heights;
    private ObjectAnimator animator1;
    private ObjectAnimator animator2;
    private int parentHeightSize;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int selfHeight;
    private boolean scrollable;

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new OverScroller(getContext());
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
            mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.FlowLayout_Layout);
        try{
            gravity = a.getInt(R.styleable.FlowLayout_Layout_android_layout_gravity,-1);
        }finally {
            a.recycle();
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        lineViews = new ArrayList<>();
        views = new ArrayList<>();
        heights = new ArrayList<>();
        int count = getChildCount();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        parentHeightSize = MeasureSpec.getSize(heightMeasureSpec);

        int lineWidth = 0;
        int lineHeight = 0;
        int totalWidth = 0;
        int totalHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            if (lp.height == MATCH_PARENT) {
                lp.height = WRAP_CONTENT;
                child.setLayoutParams(lp);
            }
            int measuredWidth = child.getMeasuredWidth();
            int measuredHeight = child.getMeasuredHeight();

            if (lineWidth + measuredWidth > widthSize) {//换行
                views.add(lineViews);
                lineViews = new ArrayList<>();
                totalWidth = Math.max(totalWidth, lineWidth);
                totalHeight += lineHeight;
                heights.add(lineHeight);
                lineWidth = 0;
                lineHeight = 0;
            }
            lineViews.add(child);
            lineWidth += measuredWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, measuredHeight + lp.topMargin + lp.bottomMargin);
        }
        views.add(lineViews);
        totalWidth = Math.max(totalWidth, lineWidth);
        totalHeight += lineHeight;
        heights.add(lineHeight);
        selfHeight = totalHeight;
        scrollable = selfHeight > parentHeightSize;
        setMeasuredDimension(widthMode == MeasureSpec.EXACTLY ? widthSize : totalWidth, heightMode == MeasureSpec.EXACTLY ? parentHeightSize : totalHeight);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = views.size();
        int top = 0;
        for (int i = 0; i < count; i++) {
            List<View> views = this.views.get(i);
            int left = 0;
            int lineTop = heights.get(i);
            for (int j = 0; j < views.size(); j++) {
                View view = views.get(j);
                MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
                int measuredWidth = view.getMeasuredWidth();
                int measuredHeight = view.getMeasuredHeight();
                view.layout(left + lp.leftMargin, top + lp.topMargin, left + measuredWidth + lp.leftMargin, top + measuredHeight + lp.topMargin);
                left += measuredWidth + lp.leftMargin + lp.rightMargin;
            }
            top += lineTop;
        }
    }

    private float mLastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!scrollable){
            return super.onTouchEvent(event);
        }
        float y = event.getY();
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = mLastY - y;//本次手势滑动了多大距离
                mScroller.startScroll(0, mScroller.getFinalY(), 0, (int) deltaY);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) mVelocityTracker.getYVelocity();
                if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                    fling(-initialVelocity);
                } else if (mScroller.springBack(0, getScrollY(), 0, 0, 0,
                        Math.max(0, selfHeight - parentHeightSize))) {
                    postInvalidateOnAnimation();
                }
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        mLastY = y;

        return true;
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0,
                    Math.max(0, selfHeight - parentHeightSize), 0, parentHeightSize / 2);

            postInvalidateOnAnimation();
        }
    }

    @Override
    public void computeScroll() {
        //先判断mScroller滚动是否完成
        if (mScroller.computeScrollOffset()) {
            //这里调用View的scrollTo()完成实际的滚动
            scrollTo(0, mScroller.getCurrY());
            postInvalidate();
        }
    }

    int preX;
    int preY;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        int y = (int) ev.getRawY();
        int x = (int) ev.getX();
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                preX = x;
                preY = y;
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int dy = Math.abs(preY - y);
                int dx = Math.abs(preX - x);
                if (dy > mTouchSlop && dy > dx){
                    intercept = true;
                }else {
                    intercept = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
        }
        preY = y;
        preX = x;
        return intercept;
    }
}
