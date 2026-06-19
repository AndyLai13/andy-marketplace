# Test Status — Current Snapshot

> **性質**：當下實測狀態。更新方式是直接覆蓋，不留歷史 batch 紀錄（git log 自己可看）。
> Canonical doctrine 在 [shape.md](./shape.md)，pattern 在 [patterns.md](./patterns.md)。
>
> 最新測量：{{TODAY}}，基準 `grep -cE "^\s*(it|test)\("` 全 tier 計數。
>
> **首次建立**：以下數字皆為 0。待第一輪 test 寫完跑 refresh script 後填入。

## 三層計數

| Tier | Tests | Files | 比例 | 估時 |
|---|---:|---:|---:|---|
| **Unit** | 0 | 0 | **0%** | — |
| **Integration** | 0 | 0 | **0%** | — |
| **E2E** | 0 | 0 | **0%** | — |
| **總計** | **0** | 0 | 100% | |

## 形狀

![Test pyramid comparison](./test-pyramid-shapes.svg)

| 模型 | Unit | Integration | E2E |
|---|---:|---:|---:|
| 理想 Pyramid（doctrine） | 70% | 20% | 10% |
| 本專案目標（[shape.md](./shape.md)） | 65% | 30% | 5% |
| **當前實測** | **0%** | **0%** | **0%** |

## 領域分布

待第一輪測試後填入。範例：

```
44  <module-a>             簡短說明
35  <module-b>             簡短說明
 ...
```

Aggregate by area：尚無資料

## 覆蓋率

| Group | Files | Lines | Functions | Branches |
|---|---:|---:|---:|---:|
| 純 helper lib | 0 | —% | —% | —% |
| Vendor adapter | 0 | —% | —% | —% |
| 資料正規化 | 0 | —% | —% | —% |
| **TOTAL** | **0** | **—%** | **—%** | **—%** |

API handler 系 v8 跨 process limitation 報 0%，靠 integration 雙保險 — 詳見 [coverage.md](./coverage.md)。

## 健康度

| 指標 | 結果 |
|---|---|
| Pass 率 | 待測 |
| Time budget | 待測 |
| 跨 tier 重複 | 待測 |
| 變動成本 | 待測 |
| 0% lib 漏網 | 待測 |
| < 90% branch lib 檔 | 待測 |

## 仍缺

1. 等第一輪測試跑完後填入。

## 怎麼更新這份

```bash
# 1. 量
npx vitest run --coverage
# (coverage 寫入 coverage/coverage-summary.json)

# 2. 數
unit=$(grep -cE "^\s*(it|test)\(" functions/lib/**/*.test.ts functions/api/**/*.test.ts tests/unit/*.test.ts 2>/dev/null | awk -F: '{sum += $2} END {print sum}')
int=$(grep -cE "^\s*(it|test)\(" tests/integration/*.test.ts | awk -F: '{sum += $2} END {print sum}')
e2e=$(grep -cE "^\s*(test|it)\(" tests/e2e/*.spec.ts | awk -F: '{sum += $2} END {print sum}')

# 3. 改這份檔的數字 + SVG 圖（test-pyramid-shapes.svg）
# 4. commit + push（不開新檔，覆蓋這份就好）
```
