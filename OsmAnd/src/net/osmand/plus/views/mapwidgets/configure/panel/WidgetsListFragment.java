package net.osmand.plus.views.mapwidgets.configure.panel;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DISABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

public class WidgetsListFragment extends Fragment implements OnScrollChangedListener {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;

	private View view;
	private View changeOrderListButton;
	private View changeOrderFooterButton;
	private ViewGroup enabledWidgetsContainer;
	private ViewGroup availableWidgetsContainer;

	private boolean nightMode;

	public void setSelectedPanel(@NonNull WidgetsPanel panel) {
		this.selectedPanel = panel;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		settings = app.getSettings();
		selectedAppMode = settings.getApplicationMode();
		nightMode = !settings.isLightContent();
		if (savedInstanceState != null) {
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_GROUP_ATTR));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		view = inflater.inflate(R.layout.fragment_widgets_list, container, false);

		enabledWidgetsContainer = view.findViewById(R.id.enabled_widgets_list);
		availableWidgetsContainer = view.findViewById(R.id.available_widgets_list);
		changeOrderListButton = view.findViewById(R.id.change_order_button_in_list);
		changeOrderFooterButton = view.findViewById(R.id.change_order_button_in_bottom);

		NestedScrollView scrollView = view.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);

		updateContent();

		TextView panelTitle = view.findViewById(R.id.panel_title);
		panelTitle.setText(getString(selectedPanel.getTitleId()));

		setupReorderButton(changeOrderListButton);
		setupReorderButton(changeOrderFooterButton);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(null);
		}
	}

	public void setupReorderButton(@NonNull View view) {
		view.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ReorderWidgetsFragment.showInstance(activity.getSupportFragmentManager(), selectedPanel,
						selectedAppMode, getParentFragment());
			}
		});
		setupListItemBackground(view);
	}

	public void updateContent() {
		enabledWidgetsContainer.removeAllViews();
		availableWidgetsContainer.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);

		int enabledAvailableFilter = ENABLED_MODE | AVAILABLE_MODE;
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(selectedAppMode, selectedPanel, enabledAvailableFilter);
		boolean noEnabledWidgets = Algorithms.isEmpty(enabledWidgets);

		View noWidgetsContainer = view.findViewById(R.id.no_widgets_container);
		AndroidUiHelper.updateVisibility(noWidgetsContainer, noEnabledWidgets);
		if (noEnabledWidgets) {
			ImageView noWidgetsImage = view.findViewById(R.id.no_widgets_image);
			Drawable noWidgetsIcon = app.getUIUtilities().getIcon(selectedPanel.getIconId(), nightMode);
			noWidgetsImage.setImageDrawable(noWidgetsIcon);
		} else {
			inflateEnabledWidgetItems(inflater);
		}

		int disabledAvailableFilter = AVAILABLE_MODE | DISABLED_MODE;
		Set<MapWidgetInfo> disabledWidgets = widgetRegistry
				.getWidgetsForPanel(selectedAppMode, selectedPanel, disabledAvailableFilter);
		boolean allWidgetsEnabled = Algorithms.isEmpty(disabledWidgets);

		View availableWidgetsCard = view.findViewById(R.id.available_widgets_container);
		AndroidUiHelper.updateVisibility(availableWidgetsCard, !allWidgetsEnabled);
		if (!allWidgetsEnabled) {
			inflateWidgetItemsViews(disabledWidgets, inflater, availableWidgetsContainer,
					R.layout.configure_screen_list_item_available_widget);
		}
	}

	private void inflateEnabledWidgetItems(@NonNull LayoutInflater inflater) {
		int enabledLayoutId = R.layout.configure_screen_widget_item;
		int enabledAvailableFilter = AVAILABLE_MODE | ENABLED_MODE;
		if (selectedPanel.isPagingAllowed()) {
			List<Set<MapWidgetInfo>> pagedWidgets =
					widgetRegistry.getPagedWidgetsForPanel(selectedAppMode, selectedPanel, enabledAvailableFilter);
			for (int i = 0; i < pagedWidgets.size(); i++) {
				inflatePageItemView(i, inflater);
				inflateWidgetItemsViews(pagedWidgets.get(i), inflater, enabledWidgetsContainer, enabledLayoutId);
			}
		} else {
			Set<MapWidgetInfo> widgets = widgetRegistry
					.getWidgetsForPanel(selectedAppMode, selectedPanel, enabledAvailableFilter);
			inflateWidgetItemsViews(widgets, inflater, enabledWidgetsContainer, enabledLayoutId);
		}
	}

	private void inflatePageItemView(int index, @NonNull LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.configure_screen_list_item_page, enabledWidgetsContainer, false);
		View topDivider = view.findViewById(R.id.top_divider);
		TextView pageText = view.findViewById(R.id.page);
		AndroidUiHelper.updateVisibility(topDivider, index > 0);
		pageText.setText(getString(R.string.page_number, String.valueOf(index + 1)));
		enabledWidgetsContainer.addView(view);
	}

	private void inflateWidgetItemsViews(@NonNull Set<MapWidgetInfo> widgetsInfo,
	                                     @NonNull LayoutInflater inflater,
	                                     @NonNull ViewGroup container,
	                                     @LayoutRes int layoutId) {
		int profileColor = selectedAppMode.getProfileColor(nightMode);
		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		List<MapWidgetInfo> widgets = new ArrayList<>(widgetsInfo);

		for (int i = 0; i < widgets.size(); i++) {
			MapWidgetInfo widgetInfo = widgets.get(i);

			View view = inflater.inflate(layoutId, container, false);

			TextView title = view.findViewById(R.id.title);
			title.setText(widgetInfo.getTitle(app));
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);

			ImageView imageView = view.findViewById(R.id.icon);
			AddedWidgetViewHolder.updateWidgetIcon(imageView, widgetInfo, profileColor, defaultIconColor, true, nightMode);

			ImageView secondaryIcon = view.findViewById(R.id.secondary_icon);
			if (secondaryIcon != null) {
				secondaryIcon.setImageResource(R.drawable.ic_action_additional_option);
				AndroidUiHelper.updateVisibility(secondaryIcon, widgetInfo.getWidgetState() != null);

				view.setOnClickListener(v -> {
					CallbackWithObject<WidgetState> callback = result -> {
						updateContent();
						return true;
					};
					widgetRegistry.showPopUpMenu(view, callback, widgetInfo.getWidgetState(), selectedAppMode,
							true, nightMode);
				});
				setupListItemBackground(view);
			}

			View bottomDivider = view.findViewById(R.id.bottom_divider);
			boolean last = i + 1 == widgets.size();
			AndroidUiHelper.updateVisibility(bottomDivider, !last);

			container.addView(view);
		}
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int activeColor = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
	}

	@Override
	public void onScrollChanged() {
		int y1 = AndroidUtils.getViewOnScreenY(changeOrderListButton);
		int y2 = AndroidUtils.getViewOnScreenY(changeOrderFooterButton);
		changeOrderFooterButton.setVisibility(y1 <= y2 ? View.GONE : View.VISIBLE);
	}
}