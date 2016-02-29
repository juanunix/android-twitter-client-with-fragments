package com.shrikant.mytwitter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.bumptech.glide.Glide;
import com.loopj.android.http.TextHttpResponseHandler;
import com.shrikant.mytwitter.tweetmodels.Tweet;

import org.apache.http.Header;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by spandhare on 2/21/16.
 */
public class ComposeFragment  extends DialogFragment {

    // Define the listener of the interface type
    // listener will the activity instance containing fragment
    private OnTweetComposedListener mOnTweetComposedListener;

    // Define the events that the fragment will use to communicate
    public interface OnTweetComposedListener {
        // This can be any number of events to be sent to the activity
        public void updateStatus(Tweet composeTweet );
    }

    private TwitterClient mTwitterClient;
    @Bind(R.id.btnTweetSent) Button mButtonTweetSend;
    @Bind(R.id.tvCharacterCount) TextView mTextViewCharCount;
    @Bind(R.id.etComposeBody) EditText mEditTextComposeBody;
    @Bind(R.id.ivComposeUserProfileImage) ImageView mImageViewUserProfileImage;
    @Bind(R.id.ll_in_reply_to) LinearLayout mLinearLayoutReplyTo;
    @Bind(R.id.ivInReplyToArrow) ImageView mImageViewInReplyToArrow;
    @Bind(R.id.tvInReplyToText) TextView mTextViewInReplyToText;

    public static ComposeFragment newInstance(String userProfileUrl) {
        ComposeFragment composeFragment = new ComposeFragment();
        Bundle args = new Bundle();
        args.putString("user_profile_url", userProfileUrl);
        composeFragment.setArguments(args);

        return composeFragment;
    }


    public static ComposeFragment newInstance(boolean isReplyTo, String inReplyToStatusId,
                                              String recipientScreenName,
                                              String userProfileUrl) {
        ComposeFragment composeFragment = new ComposeFragment();
        Bundle args = new Bundle();
        args.putBoolean("in_reply_to", isReplyTo);
        args.putString("in_reply_to_status_id", inReplyToStatusId);
        args.putString("to_screen_name", recipientScreenName);
        args.putString("user_profile_url", userProfileUrl);
        composeFragment.setArguments(args);

        return composeFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.content_compose, container, false);
        ButterKnife.bind(this, rootView);

        //getDialog().setTitle("Update Status");

        if (getArguments().containsKey("in_reply_to") && getArguments().getBoolean("in_reply_to")) {
            mLinearLayoutReplyTo.setVisibility(LinearLayout.VISIBLE);

            if (getArguments().containsKey("to_screen_name")) {
                String replyTextSet = mTextViewInReplyToText.getText().toString();

                mTextViewInReplyToText.setText(replyTextSet + " " +
                        getArguments().getString("to_screen_name"));
            }
        }

        mTwitterClient = TwitterApplication.getRestClient();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTextViewCharCount.setTextColor(Color.BLACK);
        mEditTextComposeBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                long length = 0;
                if (s.length() > 140) {
                    mTextViewCharCount.setTextColor(Color.RED);
                    length = 140 - s.length();
                    mButtonTweetSend.setEnabled(false);
                } else {
                    mTextViewCharCount.setTextColor(Color.BLACK);
                    length = s.length();
                    mButtonTweetSend.setEnabled(true);
                }

                mTextViewCharCount.setText("" + length);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

//        if (TimelineActivity.me != null && !TextUtils.isEmpty(TimelineActivity.me.getMyProfileImageUrl())) {
//            Glide.with(getContext()).load(TimelineActivity.me.getMyProfileImageUrl())
//                    .placeholder(R.mipmap.ic_wifi)
//                    .fitCenter()
//                    .into(mImageViewUserProfileImage);
//        }

        String userProfileUrl = getArguments().getString("user_profile_url");
        if (!TextUtils.isEmpty(userProfileUrl)) {
            Glide.with(getContext()).load(userProfileUrl)
                    .placeholder(R.mipmap.ic_wifi)
                    .error(R.mipmap.ic_missing_user)
                    .fitCenter()
                    .into(mImageViewUserProfileImage);

        }
    }

    @OnClick(R.id.btnTweetSent)
    public void sendTweet (View view){
        String tweetText = mEditTextComposeBody.getText().toString();
        boolean isReplyTo = false;
        String inReplyToStatusId ="";

        if (getArguments().containsKey("in_reply_to") && getArguments().getBoolean("in_reply_to")) {
            isReplyTo = true;
            if (getArguments().containsKey("in_reply_to_status_id")) {
                inReplyToStatusId = getArguments().getString("in_reply_to_status_id");

                if (getArguments().containsKey("to_screen_name")) {
                    tweetText = getArguments().getString("to_screen_name") + " " + tweetText;
                    Log.i("ComposeFragment", "Composing tweet as " + tweetText);
                }
            }
        }

        mTwitterClient.sendTweet(tweetText, isReplyTo, inReplyToStatusId, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Tweet composeTweet = new Tweet();
                if (responseString != null) {
                    Log.i("ComposeFragment", responseString);
                    try {
                        Gson gson = new GsonBuilder().create();
                        JsonObject jsonObject = gson.fromJson(responseString, JsonObject.class);
                        if (jsonObject != null) {
                            composeTweet = Tweet.fromJsonObjectToTweet(jsonObject);
                            Log.i("ComposeFragment", composeTweet.getText());
                        }
                    } catch (JsonParseException e) {
                        Log.d("Compose tweet onSuccess", "Json parsing error:" + e.getMessage(), e);
                    }
                }
                //((TimelineActivity) getActivity()).updateStatus(composeTweet);
                mOnTweetComposedListener.updateStatus(composeTweet);
                dismiss();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString,
                                  Throwable throwable) {
                Log.w("ComposeActivity", "HTTP Request failure: " + statusCode + " " +
                        throwable.getMessage());
            }
        });
    }

    @OnClick(R.id.ibDismiss)
    public void dismissFragment(View view) {
        dismiss();
    }

    // Store the listener (activity) that will have events fired once the fragment is attached
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnTweetComposedListener) {
            mOnTweetComposedListener = (OnTweetComposedListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement ComposeFragment.OnTweetComposedListener");
        }
    }
}
