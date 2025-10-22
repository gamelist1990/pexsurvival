package org.pexserver.koukunn.pexsurvival.Core.Feature;

/**
 * Feature システム - 機能実装ガイド
 * 
 * ===== 概要 =====
 * 
 * Feature インターフェースを実装することで、
 * イベントベースの機能を簡単に作成・管理できます。
 * 
 * ===== 実装例 =====
 * 
 * public class MyFeature implements Feature {
 *     private boolean enabled = false;
 * 
 *     @Override
 *     public String getFeatureName() {
 *         return "myfeature";  // 機能名（/pex toggle <name>）
 *     }
 * 
 *     @Override
 *     public String getDescription() {
 *         return "説明文";
 *     }
 * 
 *     @Override
 *     public boolean isEnabled() {
 *         return enabled;
 *     }
 * 
 *     @Override
 *     public void enable() {
 *         this.enabled = true;
 *     }
 * 
 *     @Override
 *     public void disable() {
 *         this.enabled = false;
 *     }
 * 
 *     @Override
 *     public void reload() {
 *         // リロード処理（必要なら実装）
 *     }
 * 
 *     // イベントハンドル例
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         if (!enabled) return;
 *         // 機能有効時の処理
 *     }
 * }
 * 
 * ===== 登録方法 =====
 * 
 * Loader.java の registerFeatures() メソッド内で:
 * 
 * featureManager.registerFeature(new MyFeature());
 * 
 * または複数を一括登録:
 * 
 * featureManager.registerAll(
 *     new MyFeature(),
 *     new MyFeature2(),
 *     new MyFeature3()
 * );
 * 
 * ===== コマンド操作 =====
 * 
 * 機能を切り替え:
 * /pex toggle <機能名>
 * 例: /pex toggle nojump
 * 
 * 登録済み機能一覧表示:
 * /pex list
 * 
 * 機能をリロード:
 * /pex reload <機能名>
 * 例: /pex reload nojump
 * 
 * ===== ベストプラクティス =====
 * 
 * 1. enabled フラグで機能の有効/無効を管理
 * 2. すべてのイベントハンドルで enabled チェック
 * 3. reload() で設定や状態を再初期化
 * 4. 機能名は小文字のみ使用
 * 
 * 例:
 * @EventHandler
 * public void onSomeEvent(SomeEvent event) {
 *     if (!enabled) return;  // 必須！
 *     // イベント処理
 * }
 * 
 * ===== Feature インターフェース =====
 * 
 * String getFeatureName()
 *   - 機能の名前を返す（/pex toggle で使用）
 * 
 * String getDescription()
 *   - 機能の説明を返す
 * 
 * boolean isEnabled()
 *   - 機能が有効かどうかを返す
 * 
 * void enable()
 *   - 機能を有効にする
 * 
 * void disable()
 *   - 機能を無効にする
 * 
 * void reload()
 *   - 機能をリロードする
 * 
 * Listener インターフェースも実装（イベントハンドルのため）
 *   @EventHandler アノテーション付きメソッドで処理
 */
public class FeatureGuide {
    // これはガイドファイルです
}
