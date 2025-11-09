# pexsurvival

PEX Survival Plugin - Minecraft Paper用のサバイバル強化プラグイン

## 概要

pexsurvivalは、Minecraftのサバイバル体験をよりエキサイティングにするためのPaperプラグインです。様々な自然災害や特殊な機能を追加し、より挑戦的なゲームプレイを提供します。

## 機能

### 🌀 自然災害システム (Natural Disaster)
ランダムに発生する自然災害機能を搭載：

- **火災災害 (Fire Disaster)** - 世界に炎を広げます
- **落雷災害 (Lightning Disaster)** - 雷を発生させます
- **地震災害 (Earthquake Disaster)** - 地面を揺さぶります
- **陥没穴災害 (Sinkhole Disaster)** - 地面に穴を作ります
- **砂嵐災害 (Sandstorm Disaster)** - 砂嵐を起こします
- **毒霧災害 (Toxic Fog Disaster)** - 有毒な霧を生成します
- **モブパニック災害 (Mob Panic Disaster)** - モブをパニック状態にします
- **ランダムブロック災害 (Random Block Disaster)** - ブロックをランダムに変化させます

### 💥 モブ爆発機能 (Mob Boom)
モブが爆発する機能を追加します。設定で有効/無効を切り替えられます。

### 🚫 ジャンプ禁止機能 (No Jump)
プレイヤーのジャンプを制限する機能です。

## 動作環境

- **Minecraft**: 1.21.x
- **Paper**: 1.21.8-R0.1-SNAPSHOT以上
- **Java**: 21以上

## インストール

1. 最新のリリースからプラグインのjarファイルをダウンロードしてください。
2. `plugins`フォルダにjarファイルを配置します。
3. サーバーを再起動してください。

## 設定

プラグインは初回起動時に`plugins/PEXSurvival/config.json`に設定ファイルを作成します。各機能の有効/無効やパラメータをここで調整できます。

### 設定ファイルの場所
```
plugins/
└── PEXSurvival/
    └── config.json
```

## コマンド

### `/pex help`
利用可能なコマンドの一覧を表示します。

### `/pex <機能名>`
特定の機能を有効/無効に切り替えます。

利用可能な機能名:
- `naturaldisaster` - 自然災害システム
- `mobboom` - モブ爆発機能
- `nojump` - ジャンプ禁止機能

#### 例
```
/pex naturaldisaster on  # 自然災害を有効化
/pex mobboom off        # モブ爆発を無効化
/pex nojump toggle      # ジャンプ禁止のオン/オフを切り替え
```

## 開発環境

### ビルド要件
- **Java**: OpenJDK 21以上
- **Gradle**: ラッパー使用可能

### ビルド手順
```bash
# 依存関係をダウンロード
./gradlew build

# Paperサーバーでテスト実行
./gradlew runServer
```

### プロジェクト構造
```
src/main/java/org/pexserver/koukunn/pexsurvival/
├── Core/           # コアシステム
│   ├── Command/    # コマンド処理
│   ├── Config/     # 設定管理
│   └── Feature/    # 機能管理
├── Commands/       # コマンド実装
├── Module/         # 機能モジュール
└── Loader.java     # メインクラス
```

## ライセンス

Copyright (c) 2024 Koukunn. All rights reserved.

## サポート

バグ報告や機能要望は[Issues](../../issues)でお願いします。

## 注意事項

- このプラグインはMinecraft Paper向けに開発されています。他のサーバーソフトウェアでは動作しません。
- バックアップを取らずに本番サーバーに導入しないことを推奨します。