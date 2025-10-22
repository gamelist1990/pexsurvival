# Feature システム - 完全ガイド

## 概要

**Feature システム**は、イベントベースの機能を簡単に作成・管理できるシステムです。
`/pex` コマンドで機能の有効/無効を動的に切り替えられます。

## アーキテクチャ

```
Core/
├── Feature.java          # 機能インターフェース
├── FeatureManager.java   # 機能管理システム
└── FeatureGuide.java     # 実装ガイド

Commands/
└── Pex/
    └── PexCommand.java   # /pex コマンド

Module/
├── NoJump/
│   └── NoJumpFeature.java        # ノージャンプ機能
└── Examples/
    └── WelcomeFeature.java       # ウェルカムメッセージ機能
```

## Feature インターフェース

### 実装必須メソッド

```java
public interface Feature extends Listener {
    String getFeatureName();    // 機能名
    String getDescription();    // 機能の説明
    boolean isEnabled();        // 有効かどうか
    void enable();              // 有効にする
    void disable();             // 無効にする
    void reload();              // リロード
}
```

## クイックスタート

### Step 1: Feature を実装

```java
public class MyFeature implements Feature {
    private boolean enabled = false;

    @Override
    public String getFeatureName() {
        return "myfeature";  // 機能名（小文字）
    }

    @Override
    public String getDescription() {
        return "説明文";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
        // リロード処理（必要なら実装）
    }

    // イベントハンドル
    @EventHandler
    public void onSomeEvent(SomeEvent event) {
        if (!enabled) return;  // 必須チェック
        // 機能固有の処理
    }
}
```

### Step 2: Loader に登録

```java
private void registerFeatures() {
    featureManager.registerFeature(new MyFeature());
    featureManager.registerFeature(new MyFeature2());
}
```

### Step 3: コマンドで操作

```
/pex toggle myfeature      # 有効/無効を切り替え
/pex list                  # 登録済み機能一覧
/pex reload myfeature      # 機能をリロード
```

## /pex コマンド仕様（ソース参照: `PexCommand.java`）

`/pex` はプラグインの Feature を動的に操作するための管理コマンドです（`PermissionLevel.ADMIN` による制限）。

### toggle - 機能を切り替え

```
/pex toggle <機能名>
```

使用例:

```
/pex toggle nojump    # ノージャンプ機能を切り替え
```

結果:
- 有効 → 無効
- 無効 → 有効

### list - 登録済み機能一覧

```
/pex list
```

出力例（PexCommand の実装に基づく）:

```
§b========== 登録済み機能一覧 ==========
§e• nojump §a有効 §f- ワールド全員のジャンプを禁止
§b========================================
```

### reload - 機能をリロード

```
/pex reload <機能名>
```

使用例:

```
/pex reload nojump    # ノージャンプ機能をリロード（Feature.reload() を呼ぶ）
```

## 実装例: ノージャンプ機能（現行コード）

ソース: `src/main/java/org/pexserver/koukunn/pexsurvival/Module/NoJump/NoJumpFeature.java`

このプラグインに含まれる `NoJumpFeature` は、イベントリスナーとして登録され、`PlayerToggleFlightEvent` を監視してジャンプ（フライト切り替え）を抑止します。初期状態は無効（enable フラグは false）ですが、`FeatureManager.registerFeature()` 呼び出し時に自動で enable されます。`

運用上のポイント:
- `Feature.isEnabled()` を必ずイベント処理の先頭でチェックする（実装済み）。
- `Feature.reload()` は現状空実装だが、将来的に設定再読み込みやキャッシュリセットを行う場所として使用可能。

登録/利用手順:
1. `Loader.registerFeatures()` にて `featureManager.registerFeature(new NoJumpFeature())` が呼ばれていることを確認。
2. `/pex list` で登録済み機能を確認可能。
3. `/pex toggle nojump` で有効/無効を切り替え、`/pex reload nojump` で `reload()` を呼び出せます。

## 実装例 2: カスタム機能

```java
public class AntiSpamFeature implements Feature {
    private boolean enabled = false;
    private Map<Player, Long> lastMessageTime = new HashMap<>();
    private long cooldownMs = 1000;  // 1秒

    @Override
    public String getFeatureName() {
        return "antispam";
    }

    @Override
    public String getDescription() {
        return "スパムメッセージを防止";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
        lastMessageTime.clear();
    }

    @Override
    public void reload() {
        // 設定を再読み込み
        cooldownMs = 1000;
        lastMessageTime.clear();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long last = lastMessageTime.getOrDefault(player, 0L);

        if (now - last < cooldownMs) {
            event.setCancelled(true);
            player.sendMessage("§cメッセージの送信が早すぎます");
            return;
        }

        lastMessageTime.put(player, now);
    }
}
```

## FeatureManager API

### 機能登録

```java
// 単一登録
featureManager.registerFeature(new MyFeature());

// 複数登録
featureManager.registerAll(
    new MyFeature1(),
    new MyFeature2(),
    new MyFeature3()
);
```

### 機能操作

```java
// 有効/無効を取得
Feature feature = featureManager.getFeature("myfeature");
boolean enabled = feature.isEnabled();

// 有効にする
featureManager.enableFeature("myfeature");

// 無効にする
featureManager.disableFeature("myfeature");

// トグル（有効/無効を切り替え）
featureManager.toggleFeature("myfeature");

// リロード
featureManager.reloadFeature("myfeature");

// すべてリロード
featureManager.reloadAll();

// すべて無効化
featureManager.disableAll();
```

### 機能確認

```java
// 存在確認
boolean exists = featureManager.hasFeature("myfeature");

// 全機能取得
Map<String, Feature> allFeatures = featureManager.getFeatures();

// 特定機能取得
Feature feature = featureManager.getFeature("myfeature");
```

## ベストプラクティス

### ✅ 推奨: enabled チェック

```java
@EventHandler
public void onSomeEvent(SomeEvent event) {
    if (!enabled) return;  // 必須！
    // 処理
}
```

### ✅ 推奨: enable/disable で状態管理

```java
@Override
public void enable() {
    this.enabled = true;
    // 初期化処理があれば実行
}

@Override
public void disable() {
    this.enabled = false;
    // クリーンアップ処理
}
```

### ✅ 推奨: reload で再初期化

```java
@Override
public void reload() {
    // 設定ファイルを再読み込み
    // 状態をリセット
    // 統計情報をクリア
}
```

### ❌ 非推奨: 設定ファイルがない

```java
@Override
public void reload() {
    // 何もしない
}
```

設定ファイルが必要な場合は実装してください。

## エラーハンドリング

### 機能が登録されていない

```
/pex toggle unknown
→ エラー: 機能が見つかりません: unknown
```

### 権限がない

```
/pex toggle nojump
→ エラー: 権限がありません: 管理者のみ
```

（非Op プレイヤーが実行時）

## 設定ファイルと連携

Feature を設定ファイル連携させる例：

```java
public class MyConfigFeature implements Feature {
    private boolean enabled = false;
    private FileConfiguration config;
    private Plugin plugin;

    public MyConfigFeature(Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public void reload() {
        // 設定を再読み込み
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        // 設定値を再適用
    }

    // その他のメソッド...
}
```

## チェックリスト

- [ ] Feature インターフェースを実装している
- [ ] すべてのイベントハンドルで `if (!enabled) return;` をチェック
- [ ] `enable()` で初期化処理を実装
- [ ] `disable()` でクリーンアップ処理を実装
- [ ] `reload()` で再初期化処理を実装
- [ ] Loader の `registerFeatures()` で登録している
- [ ] テストサーバーで `/pex list` で確認している
- [ ] `/pex toggle` で正しく有効/無効が切り替わるか確認している

## ファイル構成の推奨例

```
Module/
├── NoJump/
│   └── NoJumpFeature.java
├── AntiSpam/
│   └── AntiSpamFeature.java
├── Welcome/
│   └── WelcomeFeature.java
└── Examples/
    ├── AdminSettingsCommand.java
    └── ServerMaintenanceCommand.java
```

## トラブルシューティング

### 機能が有効にならない

✓ `enable()` メソッドで `this.enabled = true` を設定しているか確認
✓ イベントハンドルで `if (!enabled) return;` をチェックしているか確認

### コマンドで機能が表示されない

✓ `Loader.registerFeatures()` で登録しているか確認
✓ `getFeatureName()` が正しいか確認（小文字推奨）

### イベントが発火しない

✓ Feature が Listener を実装しているか確認
✓ `@EventHandler` アノテーションが付いているか確認
✓ イベント型が正しいか確認

## 今後の拡張予定

- [ ] 設定ファイルサポート
- [ ] 機能間の依存関係管理
- [ ] 機能の自動有効化ルール
- [ ] メトリクス・統計情報の記録

---

**Feature システム v1.0**
**Paper 1.21+ 対応**
