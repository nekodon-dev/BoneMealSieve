# Bone Meal Sieve — Fabric 26.1.2

Author: `nekodon`

Modrinth: [Bone Meal Sieve](https://modrinth.com/mod/bone-meal-sieve)

## English

### Overview

Bone Meal Sieve is a Fabric mod that uses a placeable sieve to convert normal bone meal into Low-Quality Bone Meal or High-Quality Bone Meal.

### Usage

Right-click the sieve while holding normal bone meal to insert one item. Each item takes 20 game ticks (normally one second) to process. Inserted bone meal is processed one item at a time, and the result drops from beneath the mesh. Bone meal is not consumed in Creative mode. The sieve does nothing when used with an empty hand or another item.

The sieve can hold up to one stack of input. If it is full, right-clicking it does not consume the held bone meal.

### Hopper automation

```text
Upper hopper (pointing down, containing normal bone meal)
    ↓
Bone Meal Sieve (processes one item every 20 ticks)
    ↓
Lower hopper (collects the result directly)
```

The sieve accepts only normal bone meal from the top and exposes only processed results from the bottom. When a hopper is below the sieve, results are inserted directly instead of dropping into the world. If the lower hopper is full or disabled by redstone, the sieve holds the result and waits. Processing resumes when the hopper can accept the item, without rerolling the result or consuming another input item.

Removing the lower hopper causes the held result to drop beneath the mesh. Stored items and processing progress are saved with the world. Breaking the sieve also drops its stored items.

### Block properties

- Placing the sieve does not turn the grass block beneath it into dirt.
- Hardness is 2.0 and blast resistance is 3.0, matching oak planks. An axe is the preferred tool.
- A placed sieve does not catch fire from fire or lava.
- A dropped sieve item is destroyed in lava like other items.
- The sieve is waterloggable and does not create an air pocket when placed underwater.
- Its frame, legs, and breaking particles use `minecraft:block/oak_planks`, including textures supplied by resource packs.

### Item availability

- Bone Meal Sieve appears in the Functional Blocks creative tab.
- Low-Quality Bone Meal and High-Quality Bone Meal appear in the Tools & Utilities creative tab.
- All three items are searchable in Creative mode and can be indexed by JEI.
- The sieve uses a normal crafting-table recipe. It does not add a separate JEI processing category.

### Bone meal behavior

Low-Quality Bone Meal and High-Quality Bone Meal behave as shown below. Targets not listed here, including netherrack, use vanilla bone meal behavior.

| Target | Low-Quality Bone Meal | High-Quality Bone Meal |
|---|---|---|
| Grass block | Runs generation once and removes newly generated flowers | Runs generation three times and removes newly generated non-flower plants |
| Crimson nylium | Keeps only crimson roots | Keeps only fungi and runs generation three times |
| Warped nylium | Keeps only warped roots | Keeps nether sprouts, fungi, and twisting vines and runs generation three times |
| Moss block and pale moss block | Removes azaleas and flowering azaleas | Removes short grass and tall grass |
| Saplings, azaleas, and planted mangrove propagules | Vanilla behavior | 90% chance to grow |
| Mushrooms and nether fungi | Vanilla behavior | 90% chance to grow |
| Crops | Vanilla behavior | Applies two normal growth actions |
| Small dripleaf | Drops one matching item while leaving the plant in place | Vanilla behavior |
| Big dripleaf and its stem | Shrinks by one stage; at the minimum it becomes a small dripleaf, or drops as an item if it cannot be placed | Vanilla behavior |
| Sugar cane and cactus | Vanilla behavior | Grows by one block, up to a height of three |
| Common one-block flowers | Vanilla behavior | Drops one matching flower |
| Vines | Vanilla behavior | Grows one block downward |
| Lily pads | Vanilla behavior | Adds one lily pad to an adjacent valid water surface |

Common flowers are dandelions, poppies, blue orchids, alliums, azure bluets, all tulip variants, oxeye daisies, cornflowers, and lilies of the valley. Special flowers such as wither roses, torchflowers, eyeblossoms, and golden dandelions are excluded.

### Recipe

```text
String / String     / String
Planks / Iron Ingot / Planks
Stick  / Empty      / Stick
```

### Configuration

On first launch, `config/bonemeal_sieve.json` is created in the game directory.

```json
{
  "qualityBoneMealChance": 0.3
}
```

`qualityBoneMealChance` sets the chance of producing High-Quality Bone Meal from 0.0 to 1.0. For example, 0.3 means 30% and 0.5 means 50%. A value of 0.0 always produces Low-Quality Bone Meal, while 1.0 always produces High-Quality Bone Meal. Restart the game or server after changing the file. Multiplayer uses the server-side setting.

Out-of-range values, non-numeric values, and malformed JSON produce a warning and fall back to 30% without overwriting the invalid file. A missing key also uses 30%.


---

## 日本語

### 概要

設置した「骨粉ふるい」で通常の骨粉を、質の悪い骨粉または上質な骨粉に分けるFabric製MODです。

### 使い方

通常の骨粉を持ってふるいを右クリックすると1個投入し、20ゲームtick（通常1秒）で変換します。投入された骨粉は順番に1個ずつ処理され、結果は網の下から真下へ落下します。クリエイティブでは骨粉を消費しません。素手や別のアイテムではふるい処理をしません。

入力は最大1スタックまで保持し、満杯なら右クリックしても手持ちは消費しません。

### ホッパーによる自動化

```text
上のホッパー（出口を下向きにする／通常の骨粉を入れる）
    ↓
骨粉ふるい（1個20tickで処理）
    ↓
下のホッパー（結果を直接回収）
```

上面から通常の骨粉だけを受け付け、下面から処理結果だけを取り出せます。下にホッパーがある場合はアイテムを地面に落とさず直接収納します。下のホッパーが満杯またはレッドストーンで停止中の場合は結果を保持して処理を待機し、空きができて動作可能になれば再開します。結果の再抽選や追加消費は行いません。

下のホッパーを取り外すと、保持中の結果を網の下へ落とします。内部のアイテムと処理途中の経過tickはワールドに保存され、ふるいを壊すと内部のアイテムも排出します。

### ブロックの性質

- 通常設置では下の草ブロックを土にしません。
- 硬さ2.0・爆発耐性3.0でオークの板材と同じです。斧が適正ツールです。
- 設置したふるいは火や溶岩から燃え移りません。
- ドロップしたふるいアイテムは通常アイテム同様、溶岩に入れると消失します。
- 水中設置と水入りバケツでの注水に対応し、ふるいの空間に水を保持します。
- 木枠・脚・破壊パーティクルは `minecraft:block/oak_planks` を参照し、リソースパックのオークの板材テクスチャも反映されます。

### アイテムの表示

- 骨粉ふるい：クリエイティブの「機能的ブロック」タブ
- 質の悪い骨粉／上質な骨粉：「道具と実用品」タブ
- 3種類ともクリエイティブ検索の対象で、JEIのアイテム一覧への収集対象にもなります。
- ふるいのクラフトは通常の作業台レシピです。JEI専用のふるい加工カテゴリは追加していません。

### 骨粉の効果

質の悪い骨粉と上質な骨粉は、対象に応じて次のように動作します。記載のない対象とネザーラックはバニラの骨粉処理です。

| 対象 | 質の悪い骨粉 | 上質な骨粉 |
|---|---|---|
| 草ブロック | 1回生成し、新しくできた花を除去 | 3回生成し、新しくできた花以外を除去 |
| 真紅のナイリウム | 真紅の根だけを残す | 菌類だけを残して3回生成 |
| 歪んだナイリウム | 歪んだ根だけを残す | ネザースプラウト・菌類・ねじれツタを残して3回生成 |
| 苔・薄灰色の苔ブロック | ツツジ・開花したツツジを除去 | 短い草・背の高い草を除去 |
| 苗木・ツツジ・地面のマングローブの芽 | バニラ動作 | 90%で成長 |
| キノコ・ネザー菌類 | バニラ動作 | 90%で成長 |
| 作物 | バニラ動作 | 通常2回分成長 |
| 小さなドリップリーフ | 本体を残して同アイテムを1個ドロップ | バニラ動作 |
| 大きなドリップリーフ・茎 | 1段縮小。最小なら小さなドリップリーフへ変化し、設置不能ならアイテム化 | バニラ動作 |
| サトウキビ・サボテン | バニラ動作 | 1ブロック成長（最大3ブロック） |
| 一般的な1ブロック花 | バニラ動作 | 同じ花を1個ドロップ |
| 通常のツタ | バニラ動作 | 真下へ1ブロック伸長 |
| スイレンの葉 | バニラ動作 | 隣接する有効な水面へ1個増殖 |

一般的な花はタンポポ、ポピー、ヒスイラン、アリウム、ヒナソウ、各種チューリップ、フランスギク、ヤグルマギク、スズランです。ウィザーローズ、トーチフラワー、アイブロッサム、金色のタンポポなどの特殊な花は対象外です。

### レシピ

```text
糸   / 糸   / 糸
木材 / 鉄塊 / 木材
棒   / 空   / 棒
```

### JSON設定

初回起動時にゲームディレクトリの `config/bonemeal_sieve.json` が生成されます。

```json
{
  "qualityBoneMealChance": 0.3
}
```

上質な骨粉が出る確率を0.0〜1.0で指定します。0.3は30%、0.5は50%、0.0は常に質の悪い骨粉、1.0は常に上質な骨粉です。残りの確率が質の悪い骨粉になります。変更後はゲームまたはサーバーを再起動してください。マルチプレイではサーバー側の設定を使います。

範囲外・数値以外・壊れたJSONは警告ログを出して30%に戻ります。問題のファイルは上書きしません。キーがない場合も30%になります。

