package com.lody.virtual.client.core;

import android.os.Build;

import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.MethodInvocationStub;
import com.lody.virtual.client.hook.delegate.AppInstrumentation;
import com.lody.virtual.client.hook.proxies.account.AccountManagerStub;
import com.lody.virtual.client.hook.proxies.alarm.AlarmManagerStub;
import com.lody.virtual.client.hook.proxies.am.ActivityManagerStub;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.appwidget.AppWidgetManagerStub;
import com.lody.virtual.client.hook.proxies.audio.AudioManagerStub;
import com.lody.virtual.client.hook.proxies.backup.BackupManagerStub;
import com.lody.virtual.client.hook.proxies.bluetooth.BluetoothStub;
import com.lody.virtual.client.hook.proxies.clipboard.ClipBoardStub;
import com.lody.virtual.client.hook.proxies.connectivity.ConnectivityStub;
import com.lody.virtual.client.hook.proxies.content.ContentServiceStub;
import com.lody.virtual.client.hook.proxies.context_hub.ContextHubServiceStub;
import com.lody.virtual.client.hook.proxies.devicepolicy.DevicePolicyManagerStub;
import com.lody.virtual.client.hook.proxies.display.DisplayStub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.fingerprint.FingerprintManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
import com.lody.virtual.client.hook.proxies.imms.MmsStub;
import com.lody.virtual.client.hook.proxies.input.InputMethodManagerStub;
import com.lody.virtual.client.hook.proxies.isms.ISmsStub;
import com.lody.virtual.client.hook.proxies.isub.ISubStub;
import com.lody.virtual.client.hook.proxies.job.JobServiceStub;
import com.lody.virtual.client.hook.proxies.libcore.LibCoreStub;
import com.lody.virtual.client.hook.proxies.location.LocationManagerStub;
import com.lody.virtual.client.hook.proxies.media.router.MediaRouterServiceStub;
import com.lody.virtual.client.hook.proxies.media.session.SessionManagerStub;
import com.lody.virtual.client.hook.proxies.mount.MountServiceStub;
import com.lody.virtual.client.hook.proxies.network.NetworkManagementStub;
import com.lody.virtual.client.hook.proxies.notification.NotificationManagerStub;
import com.lody.virtual.client.hook.proxies.persistent_data_block.PersistentDataBlockServiceStub;
import com.lody.virtual.client.hook.proxies.phonesubinfo.PhoneSubInfoStub;
import com.lody.virtual.client.hook.proxies.pm.PackageManagerStub;
import com.lody.virtual.client.hook.proxies.power.PowerManagerStub;
import com.lody.virtual.client.hook.proxies.restriction.RestrictionStub;
import com.lody.virtual.client.hook.proxies.search.SearchManagerStub;
import com.lody.virtual.client.hook.proxies.shortcut.ShortcutServiceStub;
import com.lody.virtual.client.hook.proxies.telephony.TelephonyRegistryStub;
import com.lody.virtual.client.hook.proxies.telephony.TelephonyStub;
import com.lody.virtual.client.hook.proxies.usage.UsageStatsManagerStub;
import com.lody.virtual.client.hook.proxies.user.UserManagerStub;
import com.lody.virtual.client.hook.proxies.vibrator.VibratorStub;
import com.lody.virtual.client.hook.proxies.wifi.WifiManagerStub;
import com.lody.virtual.client.hook.proxies.wifi_scanner.WifiScannerStub;
import com.lody.virtual.client.hook.proxies.window.WindowManagerStub;
import com.lody.virtual.client.interfaces.IInjector;

import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

/**
 * @author Lody
 *
 */
public final class InvocationStubManager {

    private static InvocationStubManager sInstance = new InvocationStubManager();
    private static boolean sInit;

	private Map<Class<?>, IInjector> mInjectors = new HashMap<>(13);

	private InvocationStubManager() {
	}

	public static InvocationStubManager getInstance() {
		return sInstance;
	}

	void injectAll() throws Throwable {
		for (IInjector injector : mInjectors.values()) {
			injector.inject();
		}
		// XXX: Lazy inject the Instrumentation,
		addInjector(AppInstrumentation.getDefault());
	}

    /**
	 * @return if the InvocationStubManager has been initialized.
	 */
	public boolean isInit() {
		return sInit;
	}

	/**
	 * 系统服务hook的初始化
	 * @throws Throwable
	 */
	public void init() throws Throwable {
		if (isInit()) {
			throw new IllegalStateException("InvocationStubManager Has been initialized.");
		}
		injectInternal();
		sInit = true;

	}

	/**
	 * 将需要hook的系统服务的代理对象添加到集合中
	 * @throws Throwable
	 */
	private void injectInternal() throws Throwable {
		if (VirtualCore.get().isMainProcess()) {
			return;
		}
		if (VirtualCore.get().isServerProcess()) {
			//ActivityManager
			addInjector(new ActivityManagerStub());
			//PackageManager
			addInjector(new PackageManagerStub());
			return;
		}
		if (VirtualCore.get().isVAppProcess()) {
			//BlockGuardOs
			addInjector(new LibCoreStub());
			//ActivityManager
			addInjector(new ActivityManagerStub());
			//PackageManager
			addInjector(new PackageManagerStub());
			//ActivityThread.mH
			addInjector(HCallbackStub.getDefault());
			//ISms
			addInjector(new ISmsStub());
			//ISub
			addInjector(new ISubStub());
			//DropBoxManager
			addInjector(new DropBoxManagerStub());
			//NotificationManager
			addInjector(new NotificationManagerStub());
			//LocationManager
			addInjector(new LocationManagerStub());
			//WindowManager
			addInjector(new WindowManagerStub());
			//ClipBoard
			addInjector(new ClipBoardStub());
			//MountService
			addInjector(new MountServiceStub());
			//BackupManager
			addInjector(new BackupManagerStub());
			// Telephony
			addInjector(new TelephonyStub());
			//TelephonyRegistry
			addInjector(new TelephonyRegistryStub());
			//PhoneSubInfo
			addInjector(new PhoneSubInfoStub());
			//PowerManager
			addInjector(new PowerManagerStub());
			//AppWidgetManager
			addInjector(new AppWidgetManagerStub());
			//AccountManager
			addInjector(new AccountManagerStub());
			//AudioManager
			addInjector(new AudioManagerStub());
			//SearchManager
			addInjector(new SearchManagerStub());
			//ContentService
			addInjector(new ContentServiceStub());
			//Connectivity
			addInjector(new ConnectivityStub());

			if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR2) {
				//Vibrator
				addInjector(new VibratorStub());
				//WifiManager
				addInjector(new WifiManagerStub());
				//Bluetooth
				addInjector(new BluetoothStub());

				addInjector(new ContextHubServiceStub());
			}
			if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
				//UserManager
				addInjector(new UserManagerStub());
			}

			if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
				//Display
				addInjector(new DisplayStub());
			}
			if (Build.VERSION.SDK_INT >= LOLLIPOP) {
				//PersistentDataBlockService
				addInjector(new PersistentDataBlockServiceStub());
				//InputMethodManager
				addInjector(new InputMethodManagerStub());
				//Mms in  Telephony
				addInjector(new MmsStub());
				//SessionManager
				addInjector(new SessionManagerStub());
				//JobService
				addInjector(new JobServiceStub());
				//Restriction
				addInjector(new RestrictionStub());
			}
			if (Build.VERSION.SDK_INT >= KITKAT) {
				//AlarmManager
				addInjector(new AlarmManagerStub());
				//AppOpsManager
				addInjector(new AppOpsManagerStub());
				//MediaRouterService
				addInjector(new MediaRouterServiceStub());
			}
			if (Build.VERSION.SDK_INT >= LOLLIPOP_MR1) {
				//GraphicsStats
				addInjector(new GraphicsStatsStub());
				//UsageStatsManager
				addInjector(new UsageStatsManagerStub());
			}
			if (Build.VERSION.SDK_INT >= M) {
				//FingerprintManager
				addInjector(new FingerprintManagerStub());
				//NetworkManagement
				addInjector(new NetworkManagementStub());
			}
			if (Build.VERSION.SDK_INT >= N) {
				//WifiScanner
                addInjector(new WifiScannerStub());
                //ShortcutService
                addInjector(new ShortcutServiceStub());
                //DevicePolicyManager
                addInjector(new DevicePolicyManagerStub());
            }
		}
	}

	private void addInjector(IInjector IInjector) {
		mInjectors.put(IInjector.getClass(), IInjector);
	}

	public <T extends IInjector> T findInjector(Class<T> clazz) {
		// noinspection unchecked
		return (T) mInjectors.get(clazz);
	}

	public <T extends IInjector> void checkEnv(Class<T> clazz) {
		IInjector IInjector = findInjector(clazz);
		if (IInjector != null && IInjector.isEnvBad()) {
			try {
				IInjector.inject();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public <T extends IInjector, H extends MethodInvocationStub> H getInvocationStub(Class<T> injectorClass) {
		T injector = findInjector(injectorClass);
		if (injector != null && injector instanceof MethodInvocationProxy) {
			// noinspection unchecked
			return (H) ((MethodInvocationProxy) injector).getInvocationStub();
		}
		return null;
	}

}