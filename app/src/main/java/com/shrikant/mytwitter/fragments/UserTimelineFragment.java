package com.shrikant.mytwitter.fragments;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.loopj.android.http.TextHttpResponseHandler;
import com.shrikant.mytwitter.R;
import com.shrikant.mytwitter.adapters.ComplexRecyclerViewUserTimelineAdapter;
import com.shrikant.mytwitter.adapters.DividerItemDecoration;
import com.shrikant.mytwitter.adapters.EndlessRecyclerViewScrollListener;
import com.shrikant.mytwitter.tweetmodels.Tweet;

import org.apache.http.Header;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by spandhare on 2/27/16.
 */
public class UserTimelineFragment extends TweetsFragment {
    private LinearLayoutManager layoutManager;
    private LinkedList<Tweet> mTweets;
    private ComplexRecyclerViewUserTimelineAdapter mComplexRecyclerViewuserTimelineAdapter;

    @Bind(R.id.rvUserTimelineTweets) RecyclerView mRecyclerViewTweets;
    @Bind(R.id.swipeContainerUsertimeline) SwipeRefreshLayout mSwipeRefreshLayout;

    public static UserTimelineFragment newInstance(String screenName) {
        UserTimelineFragment userTimelineFragment = new UserTimelineFragment();
        Bundle args = new Bundle();
        args.putString("screen_name", screenName);
        userTimelineFragment.setArguments(args);
        return userTimelineFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        super.onCreateView(inflater, parent, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_usertimeline_tweets, parent, false);
        ButterKnife.bind(this, v);

        mRecyclerViewTweets.setAdapter(mComplexRecyclerViewuserTimelineAdapter);

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        mRecyclerViewTweets.addItemDecoration(itemDecoration);

        // Setup layout manager for items
        layoutManager = new LinearLayoutManager(getActivity());
        // Control orientation of the items
        // also supports LinearLayoutManager.HORIZONTAL
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        // Optionally customize the position you want to default scroll to
        layoutManager.scrollToPosition(0);
        // Set layout manager to position the items
        // Attach the layout manager to the recycler view

        mRecyclerViewTweets.setLayoutManager(layoutManager);

        mRecyclerViewTweets.addOnScrollListener(
                new EndlessRecyclerViewScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount) {
                        // Triggered only when new data needs to be appended to the list
                        // Add whatever code is needed to append new items to the bottom of the list
                        Toast.makeText(getContext(),
                                "Loading more...", Toast.LENGTH_SHORT).show();
                        // Send an API request to retrieve appropriate data using the offset value as a parameter.
                        // Deserialize API response and then construct new objects to append to the adapter
                        // Add the new objects to the data source for the adapter
                        // For efficiency purposes, notify the adapter of only the elements that got changed
                        // curSize will equal to the index of the first element inserted because the list is 0-indexed
                        populateTimeLine(true, false);

                    }
                });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                populateTimeLine(false, true);
            }
        });

        if (!isOnline()) {
            Toast.makeText(getContext(), "Your device is not online, " +
                            "check wifi and try again!",
                    Toast.LENGTH_LONG).show();
        } else {
            mTweets.clear();
            mComplexRecyclerViewuserTimelineAdapter.notifyDataSetChanged();
            //kick off realtime timelines
            populateTimeLine(false, false);
        }

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTweets = new LinkedList<>();
        mComplexRecyclerViewuserTimelineAdapter = new ComplexRecyclerViewUserTimelineAdapter(getActivity(),
                mTweets);
    }

    @Override
    void populateTimeLine(final boolean isScrolled, final boolean isRefreshed) {
        String screenName = getArguments().getString("screen_name");
        mTwitterClient.getUserTimeline(screenName,
            new TextHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    List<Tweet> fetchedTweets = new ArrayList<Tweet>();
                    if (responseString != null) {
                        Log.i("UserTimelineFragment", responseString);
                        try {
                            Gson gson = new GsonBuilder().create();
                            JsonArray jsonArray = gson.fromJson(responseString, JsonArray.class);

                            if (jsonArray != null) {

                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JsonObject jsonTweetObject = jsonArray.get(i).getAsJsonObject();

                                    if (jsonTweetObject != null) {
                                        fetchedTweets.add(Tweet.fromJsonObjectToTweet(jsonTweetObject));
                                    }
                                }
                                Log.i("UserTimelineFragment", fetchedTweets.size() + " tweets found");

                                //add to list
                                if (isRefreshed) {
                                    Log.i("UserTimelineFragment", fetchedTweets.size() + " new tweets " +
                                            "found");
                                    for (int i = fetchedTweets.size() - 1; i >= 0; i--) {
                                        mTweets.addFirst(fetchedTweets.get(i));
                                    }
                                } else {
                                    mTweets.addAll(fetchedTweets);
                                }
                                Log.i("UserTimelineFragment", mTweets.getFirst().getIdStr() + " max id");
                                Log.i("UserTimelineFragment", mTweets.getLast().getIdStr() + " since id");
                                Log.i("UserTimelineFragment", mTweets.size() + " tweets found");
                            }
                        } catch (JsonParseException e) {
                            Log.d("Async onSuccess", "Json parsing error:" + e.getMessage(), e);
                        }

                        //notify adapter
                        if (isScrolled) {
                            mComplexRecyclerViewuserTimelineAdapter.notifyItemRangeInserted(
                                    mComplexRecyclerViewuserTimelineAdapter.getItemCount(),
                                    fetchedTweets.size());
                        } else if (isRefreshed) {
                            mComplexRecyclerViewuserTimelineAdapter.notifyItemRangeInserted(0,
                                    fetchedTweets.size());
                            //layoutManager.scrollToPosition(0);
                            // Now we call setRefreshing(false) to signal refresh has finished
                            mSwipeRefreshLayout.setRefreshing(false);

                        } else {
                            mComplexRecyclerViewuserTimelineAdapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString,
                                      Throwable throwable) {
                    Log.w("AsyncHttpClient", "HTTP Request failure: " + statusCode + " " +
                            throwable.getMessage());
                }
            });

    }
}
