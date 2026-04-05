package com.liskovsoft.smartyoutubetv2.tv.ui.signin;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
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

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

public class SignInFragment extends GuidedStepSupportFragment implements SignInView {
    private static final String TAG = SignInFragment.class.getSimpleName();
    private static final int CONTINUE = 2;
    private static final int OPEN_BROWSER = 3;
    private SignInPresenter mSignInPresenter;
    private String mFullSignInUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSignInPresenter = SignInPresenter.instance(getContext());
        mSignInPresenter.setView(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Style the QR code icon view: white background with rounded corners (like PrismTube)
        styleQrCodeView();

        mSignInPresenter.onViewInitialized();
    }

    private void styleQrCodeView() {
        ImageView iconView = getGuidanceStylist().getIconView();
        if (iconView != null) {
            float density = getResources().getDisplayMetrics().density;

            // White background with rounded corners (PrismTube style)
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(12 * density);
            iconView.setBackground(bg);
            int pad = (int) (8 * density);
            iconView.setPadding(pad, pad, pad, pad);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Fixed QR code size
            ViewGroup.LayoutParams params = iconView.getLayoutParams();
            if (params != null) {
                int size = (int) (180 * density);
                params.width = size;
                params.height = size;
                iconView.setLayoutParams(params);
            }

            // Align icon with text: remove extra margins from parent
            if (iconView.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) iconView.getParent();
                parent.setPadding(0, 0, 0, 0);
                // Center the QR code in its container
                if (parent.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) parent.getLayoutParams();
                    mlp.setMargins(0, 0, (int)(16 * density), 0);
                    parent.setLayoutParams(mlp);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSignInPresenter.onViewDestroyed();
    }

    @Override
    public void showCode(String userCode, String signInUrl) {
        setTitle(userCode, signInUrl);
    }

    private void setTitle(String userCode, String signInUrl) {
        if (TextUtils.isEmpty(userCode)) {
            return;
        }

        // Large, prominent user code with letter spacing (like PrismTube)
        android.widget.TextView titleView = getGuidanceStylist().getTitleView();
        titleView.setText(userCode);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 36);
        titleView.setLetterSpacing(0.3f);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);

        mFullSignInUrl = signInUrl + "?user_code=" + userCode.replace(" ", "-");

        // URL-encode the full sign-in URL for QR code generation (like PrismTube)
        String encodedUrl;
        try {
            encodedUrl = java.net.URLEncoder.encode(mFullSignInUrl, "UTF-8");
        } catch (Exception e) {
            encodedUrl = mFullSignInUrl;
        }

        Glide.with(getContext())
                .load(Utils.toQrCodeLink(encodedUrl))
                .placeholder(R.drawable.activate_account_qrcode)
                .apply(ViewUtil.glideOptions())
                .error(R.drawable.activate_account_qrcode)
                .listener(mErrorListener)
                .into(getGuidanceStylist().getIconView());

        String description = getString(R.string.signin_view_description, signInUrl);
        int start = description.indexOf(signInUrl);
        int end = start + signInUrl.length();
        CharSequence coloredDescription = Utils.color(description, ContextCompat.getColor(getContext(), R.color.red), start, end);

        getGuidanceStylist().getDescriptionView().setText(coloredDescription);
    }

    @Override
    public void close() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.signin_view_title);
        String description = getString(R.string.signin_view_description, "");
        return new GuidanceStylist.Guidance(title, description, "", ContextCompat.getDrawable(getContext(), R.drawable.activate_account_qrcode));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction login = new GuidedAction.Builder()
                .id(CONTINUE)
                .title(getString(R.string.signin_view_action_text))
                .build();
        GuidedAction openBrowser = new GuidedAction.Builder()
                .id(OPEN_BROWSER)
                .title(getString(R.string.login_from_browser))
                .build();
        actions.add(login);
        actions.add(openBrowser);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            mSignInPresenter.onActionClicked();
        } else if (action.getId() == OPEN_BROWSER) {
            if (mFullSignInUrl != null) {
                Utils.openLinkExt(getContext(), mFullSignInUrl);
            }
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
