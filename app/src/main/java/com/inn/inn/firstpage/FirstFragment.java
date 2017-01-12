package com.inn.inn.firstpage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.inn.inn.R;
import com.inn.inn.customview.TopBarView;
import com.inn.inn.firstpage.model.DayDetail;
import com.inn.inn.firstpage.model.TimeList;
import com.inn.inn.mainpage.WelcomeActivity;
import com.inn.inn.network.InnHttpClient;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;


public class FirstFragment extends Fragment {

    private Context context;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private List<DayDetail> dayDetails = new ArrayList<>();
    private List<String> timeLists = new ArrayList<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TopBarView topBarView;
    private FirstPageRecycleViewAdapter firstPageRecycleViewAdapter;

    private int pageSize = 0;
    private int pagePosition = 20;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        View view = inflater.inflate(R.layout.first_fragment_layout, container, false);
        initView(view);
        initRecycleView();
        initListener();
        return view;
    }

    private void initView(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.first_page_recycle_view);
        topBarView = (TopBarView) view.findViewById(R.id.first_page_top_bar);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.first_page_swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.base_color);
        topBarView.setTopBarTitle("每日推荐");
    }

    private void initRecycleView() {
        firstPageRecycleViewAdapter = new FirstPageRecycleViewAdapter((Activity) context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(firstPageRecycleViewAdapter);
    }

    private void initListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                dayDetails.clear();
                pageSize = 0;
                pagePosition = 20;
                for (int i = pageSize; i < pagePosition; i++) {
                    getDayListData(timeLists.get(i));
                }
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (RecyclerView.SCROLL_STATE_IDLE == newState) {
                    if (!recyclerView.canScrollVertically(1)) {
                        loadNetData();
                    }
                }
            }
        });

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        loadNetData();
    }

    private void loadNetData() {
        swipeRefreshLayout.setRefreshing(true);
        for (int i = pageSize; i < pagePosition; i++) {
            getDayListData(timeLists.get(i));
        }
        pageSize = pageSize + 20;
        pagePosition = pagePosition + 20;
    }

    private void initData() {
        Intent intent = getActivity().getIntent();
        TimeList timeList = (TimeList) intent.getSerializableExtra(WelcomeActivity.BASE_TIME_DATA);
        timeLists = timeList.getResults();
    }

    private void getDayListData(String timeString) {
        String time = timeString.replaceAll("-", "/");
        Subscription subscription = InnHttpClient.getHttpServiceInstance().getDayList(time)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DayDetail>() {
                    @Override
                    public void call(DayDetail detail) {
                        dayDetails.add(detail);
                        firstPageRecycleViewAdapter.refreshData(dayDetails);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
        compositeSubscription.add(subscription);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeSubscription.unsubscribe();
    }
}
