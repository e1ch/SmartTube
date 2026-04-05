package com.liskovsoft.smartyoutubetv2.tv.ui.signin;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.List;

public class SignInFragment extends GuidedStepSupportFragment implements SignInView {
    private static final String TAG = SignInFragment.class.getSimpleName();
    private static final int CONTINUE = 2;
    private static final int OPEN_BROWSER = 3;
    private SignInPresenter mSignInPresenter;
    private String mFullSignInUrl;

    // Soft warm gray for QR background (easy on eyes in dark room)
    private static final int QR_BG_COLOR = 0xFFE8E8E8;
    private boolean mQrLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSignInPresenter = SignInPresenter.instance(getContext());
        mSignInPresenter.setView(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSignInPresenter.onViewInitialized();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSignInPresenter.onViewDestroyed();
    }

    @Override
    public void showCode(String userCode, String signInUrl) {
        if (TextUtils.isEmpty(userCode) || getContext() == null) return;

        mFullSignInUrl = signInUrl + "?user_code=" + userCode.replace(" ", "-");

        // QR code: load only once with the sign-in URL (code won't auto-fill via yt.be/activate)
        ImageView iconView = getGuidanceStylist().getIconView();
        if (iconView != null && !mQrLoaded) {
            mQrLoaded = true;
            float density = getResources().getDisplayMetrics().density;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(QR_BG_COLOR);
            bg.setCornerRadius(16 * density);
            iconView.setBackground(bg);
            int pad = (int) (10 * density);
            iconView.setPadding(pad, pad, pad, pad);

            // Static QR — just the activation URL, no need to refresh per code
            Glide.with(getContext())
                    .load(Utils.toQrCodeLink(signInUrl))
                    .placeholder(R.drawable.activate_account_qrcode)
                    .apply(ViewUtil.glideOptions())
                    .error(R.drawable.activate_account_qrcode)
                    .listener(mErrorListener)
                    .into(iconView);
        }

        // User code: single line, centered under QR
        TextView titleView = getGuidanceStylist().getTitleView();
        if (titleView != null) {
            titleView.setText(userCode);
        }

        // Description: sign-in URL highlighted in red
        TextView descView = getGuidanceStylist().getDescriptionView();
        if (descView != null) {
            String description = getString(R.string.signin_view_description, signInUrl);
            int start = description.indexOf(signInUrl);
            int end = start + signInUrl.length();
            descView.setText(Utils.color(description, ContextCompat.getColor(getContext(), R.color.red), start, end));
        }
    }

    @Override
    public void close() {
        if (getActivity() != null) getActivity().finish();
    }

    @Override
    @NonNull
    public GuidanceStylist onCreateGuidanceStylist() {
        return new SignInGuidanceStylist();
    }

    /**
     * Custom GuidanceStylist that uses vertical layout:
     * QR code on top → user code below → description below, all centered and 200dp-width aligned.
     */
    private static class SignInGuidanceStylist extends GuidanceStylist {
        private ImageView mIconView;
        private TextView mTitleView;
        private TextView mDescriptionView;
        private TextView mBreadcrumbView;

        @Override
        public View onCreateView(LayoutInflater inflater, android.view.ViewGroup container, Guidance guidance) {
            View view = inflater.inflate(R.layout.signin_guidance, container, false);

            mIconView = view.findViewById(R.id.guidance_icon);
            mTitleView = view.findViewById(R.id.guidance_title);
            mDescriptionView = view.findViewById(R.id.guidance_description);
            mBreadcrumbView = view.findViewById(R.id.guidance_breadcrumb);

            if (mTitleView != null) mTitleView.setText(guidance.getTitle());
            if (mDescriptionView != null) mDescriptionView.setText(guidance.getDescription());
            if (mIconView != null && guidance.getIconDrawable() != null) {
                mIconView.setImageDrawable(guidance.getIconDrawable());
            }

            // Force vertical centering: post to ensure parent is measured
            view.post(() -> {
                if (view.getParent() instanceof android.view.ViewGroup) {
                    android.view.ViewGroup parent = (android.view.ViewGroup) view.getParent();
                    // Make parent fill available height for centering to work
                    android.view.ViewGroup.LayoutParams lp = parent.getLayoutParams();
                    if (lp != null) {
                        lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                        parent.setLayoutParams(lp);
                    }
                }
            });

            return view;
        }

        @Override public ImageView getIconView() { return mIconView; }
        @Override public TextView getTitleView() { return mTitleView; }
        @Override public TextView getDescriptionView() { return mDescriptionView; }
        @Override public TextView getBreadcrumbView() { return mBreadcrumbView; }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.signin_view_title);
        String description = getString(R.string.signin_view_description, "");
        return new GuidanceStylist.Guidance(title, description, "",
                ContextCompat.getDrawable(getContext(), R.drawable.activate_account_qrcode));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder()
                .id(CONTINUE)
                .title(getString(R.string.signin_view_action_text))
                .build());
        actions.add(new GuidedAction.Builder()
                .id(OPEN_BROWSER)
                .title(getString(R.string.login_from_browser))
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            mSignInPresenter.onActionClicked();
        } else if (action.getId() == OPEN_BROWSER) {
            if (mFullSignInUrl != null) Utils.openLinkExt(getContext(), mFullSignInUrl);
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }
        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
