# PEX Survival - 動的コマンドローダー ガイド

## 概要

このプラグインは、Paper 1.21+ 対応の**動的コマンドローダー**を提供します。
コマンド実装をシンプルで直感的にできるようになりました。

## 特徴

✅ **簡単なコマンド登録** - `Command.register(クラス)` で登録可能
✅ **テンプレートベース** - BaseCommand を継承するだけ
✅ **権限レベルシステム** - 5段階の権限レベルで管理が簡単
✅ **統一されたメッセージング** - エラー・成功・情報メッセージが統一
✅ **Paper 1.21+ 対応** - 最新バージョンに完全対応

## アーキテクチャ

```
src/main/java/org/pexserver/koukunn/pexsurvival/
├── Loader.java                    # プラグイン初期化（Feature / Command の登録）
├── Core/
│   ├── BaseCommand.java           # コマンド基底クラス（テンプレート）
│   ├── CommandManager.java        # コマンド管理・登録システム（動的登録）
│   ├── PermissionLevel.java       # 権限レベル定義（Enum）
│   └── HelpCommand.java           # ヘルプコマンド（/pexhelp）
└── Module/
    └── NoJump/
        └── NoJumpFeature.java     # ノージャンプ機能（/pex で管理される Feature の例）
```

## 権限レベルシステム

### 5段階の権限レベル

| レベル | 説明 | 実行可能な者 |
|--------|------|-----------|
| **0 - ANY** | 誰でも実行可能 | 全員 |
| **1 - MEMBER** | メンバー以上 | プレイヤー + コンソール |
| **2 - ADMIN** | 管理者のみ | Op 権限を持つプレイヤーのみ |
| **3 - CONSOLE** | コンソール専用 | コンソールのみ（プレイヤー不可） |
| **4 - ADMIN_OR_CONSOLE** | 管理者またはコンソール | Op 権限 またはコンソール |

## クイックスタート（現行実装に合わせた要点）

1. `BaseCommand` を継承してコマンドを実装します。必須は `getName()`, `getDescription()`, `execute()`。
2. 権限は `getPermissionLevel()`（デフォルトは `ANY`）。必要なら `getPermission()` でカスタム権限ノードを返します。
3. `Loader.java` の `registerCommands()` で `CommandManager.register(...)` を呼ぶことで動的に登録されます。

簡易テンプレート（要点のみ）:

```java
public class MyCommand extends BaseCommand {
    @Override public String getName() { return "mycommand"; }
    @Override public String getDescription() { return "説明"; }
    @Override public PermissionLevel getPermissionLevel() { return PermissionLevel.ADMIN; }
    @Override public boolean execute(CommandSender sender, String[] args) {
        sendSuccess(sender, "実行成功");
        return true;
    }
}
```

### 2. コマンド登録

`Loader.java` の `registerCommands()` メソッド内で登録します：

```java
private void registerCommands() {
    // クラスから直接登録
    commandManager.register(MyCommand.class);
    
    // またはインスタンスから登録
    commandManager.register(new MyCommand());
    
    // 複数を一括登録
    commandManager.registerAll(
        MyCommand.class,
        MyCommand2.class,
        MyCommand3.class
    );
}
```

## BaseCommand API

### 実装必須メソッド

| メソッド | 説明 | 戻り値 |
|---------|------|-------|
| `getName()` | コマンド名 | String |
| `getDescription()` | コマンドの説明 | String |
| `execute()` | コマンド実行処理 | boolean |

### 実装オプショナルメソッド

| メソッド | 説明 | デフォルト |
|---------|------|----------|
| `getPermissionLevel()` | 権限レベル | `PermissionLevel.ANY` |
| `getPermission()` | カスタム権限キー | `null` |
| `getUsage()` | 使用法 | `"/" + getName()` |

### 保護されたユーティリティメソッド

```java
// 権限チェック（権限レベル + カスタム権限の両方を確認）
protected boolean hasPermission(CommandSender sender)

// メッセージ送信
protected void sendError(CommandSender sender, String message)      // 赤色
protected void sendSuccess(CommandSender sender, String message)    // 緑色
protected void sendInfo(CommandSender sender, String message)       // 青色
```

## CommandManager API

### 登録メソッド

```java
// 単一登録（クラス）
commandManager.register(Class<? extends BaseCommand>)

// 単一登録（インスタンス）
commandManager.register(BaseCommand)

// 複数登録（クラス）
commandManager.registerAll(Class<? extends BaseCommand>...)

// 複数登録（インスタンス）
commandManager.registerAll(BaseCommand...)
```

### 取得メソッド

```java
// 単一コマンド取得
BaseCommand cmd = commandManager.getCommand(String name)

// すべてのコマンド取得
Map<String, BaseCommand> allCommands = commandManager.getCommands()
```

## PermissionLevel の使い方

### 例1: 誰でも実行可能

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ANY;
}
```

### 例2: Op のみ実行可能

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;
}
```

### 例3: コンソールのみ実行可能

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.CONSOLE;
}
```

### 例4: カスタム権限を使用（優先度高）

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;  // フォールバック
}

@Override
public String getPermission() {
    return "pexsurvival.custom.permission";  // こちらが優先される
}
```

## 実装例

### 例1: シンプルなコマンド（誰でも実行可能）

```java
public class StatusCommand extends BaseCommand {

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getDescription() {
        return "ステータス情報を表示";
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ANY;  // 誰でも実行可能
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sendInfo(sender, "プラグインは正常に動作しています");
        return true;
    }
}
```

### 例2: 管理者専用コマンド

```java
public class SettingsCommand extends BaseCommand {

    @Override
    public String getName() {
        return "pexsettings";
    }

    @Override
    public String getDescription() {
        return "プラグイン設定を変更";
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;  // Op のみ
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "使用法: /pexsettings <設定名> <値>");
            return false;
        }

        String setting = args[0];
        String value = args[1];

        sendSuccess(sender, setting + " を " + value + " に設定しました");
        return true;
    }

    @Override
    public String getUsage() {
        return "/pexsettings <設定名> <値>";
    }
}
```

### 例3: コンソール専用コマンド

```java
public class UpdateCommand extends BaseCommand {

    @Override
    public String getName() {
        return "pexupdate";
    }

    @Override
    public String getDescription() {
        return "プラグインを更新（コンソール専用）";
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.CONSOLE;  // コンソール専用
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sendInfo(sender, "更新を確認しています...");
        // 更新チェック処理
        sendSuccess(sender, "プラグインは最新版です");
        return true;
    }
}
```

### 例4: 引数ありのコマンド（プレイヤー + コンソール）

```java
public class TeleportCommand extends BaseCommand {

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "プレイヤーをテレポート";
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.MEMBER;  // プレイヤー + コンソール
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendError(sender, "プレイヤーのみ実行可能です");
            return true;
        }

        if (args.length < 3) {
            sendError(sender, "使用法: /tp <x> <y> <z>");
            return false;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);

            Player player = (Player) sender;
            player.teleport(new Location(player.getWorld(), x, y, z));
            sendSuccess(player, "テレポート完了: " + x + ", " + y + ", " + z);
            return true;
        } catch (NumberFormatException e) {
            sendError(sender, "座標は数値で指定してください");
            return false;
        }
    }

    @Override
    public String getUsage() {
        return "/tp <x> <y> <z>";
    }
}
```

## ファイル構成

```
src/main/java/org/pexserver/koukunn/pexsurvival/
├── Loader.java                    # メインプラグインクラス
├── Core/
│   ├── BaseCommand.java           # コマンド基底クラス
│   ├── CommandManager.java        # コマンド管理システム
│   ├── PermissionLevel.java       # 権限レベル定義（Enum）
│   ├── HelpCommand.java           # ヘルプコマンド
│   └── CommandGuide.java          # 実装ガイド
└── Module/
    └── NoJump/
        └── NoJumpCommand.java     # NoJumpコマンドの実装例
```

## トラブルシューティング

### コマンドが登録されない

✓ `Loader.java` の `registerCommands()` で登録しているか確認
✓ `BaseCommand` を継承しているか確認
✓ 必須メソッド（`getName()`, `getDescription()`, `execute()`）をすべて実装しているか確認

### 権限エラーが出る

✓ `getPermissionLevel()` の設定を確認
✓ プレイヤーが Op になっているか確認（`/op <プレイヤー名>`）
✓ コンソールで実行していないか確認（CONSOLE レベルの場合）

### コマンドが実行されない

✓ コマンド名が正しいか確認（大文字小文字は区別されません）
✓ コンソールのエラーメッセージを確認
✓ `hasPermission()` が true を返しているか確認

## 今後の拡張予定

- [ ] タブコンプリート機能
- [ ] サブコマンド機能
- [ ] コマンドの遅延実行
- [ ] コマンドパラメータの型変換
- [ ] 動的権限設定

---

**作成日**: 2025年10月22日
**対応バージョン**: Paper 1.21+
**権限システムバージョン**: v2.0（PermissionLevel ベース）
