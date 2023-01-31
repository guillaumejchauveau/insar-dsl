# Benchmark

## Empty program
Input:
```json
[
  {"name": "John", "age": 42},
  {"name": "Jane", "age": 38},
  {"name": "Bob", "age": 12},
  {"name": "Dylan", "age": 21}
]
```
Output:
```csv
"name","age"
"John",42
"Jane",38
"Bob",12
"Dylan",21
```

Oui program:
```

```

Model:
```mermaid
flowchart LR
    p0[Program]
```

## Object repetition
Input:
```json
[
  {"ip": "195.66.2.37", "ports": [ {"port": 0}, {"port": 80}, {"port": 443} ]},
  {"ip": "145.38.39.15", "ports": [ {"port": 0} ]},
  {"ip": "87.116.6.182", "ports": [ {"port": 0}, {"port": 22} ]}
]
```
Output:
```csv
"ip","port"
"195.66.2.37",0
"195.66.2.37",80
"195.66.2.37",443
"145.38.39.15",0
"87.116.6.182",0
"87.116.6.182",22
```

Oui program:
```
$[] | { $.ip, $.ports[].port }
```
Model:
```mermaid
flowchart 
    Program -->|first| is0[InstructionSet] -->|instruction| is0_i
    subgraph "$[]" 
    is0_i[Selector]
    is0_i -->|scope| is0_i_s[RootSelectorScope]
    is0_i -->|fragment0| is0_i_f0[IndexSelectorFragment] -->|indexes| is0_i_f0_i([ ])
    end
    
    is0 -->|next| is1[InstructionSet] -->|instruction| is1_i
    subgraph "{ $.ip, $.ports[].port }"
    is1_i[Object]

    is1_i -->|property0| is1_i_p0[ObjectProperty]
    is1_i_p0 -->|key| is1_i_p0_k([ ])
    is1_i_p0 -->|value| is1_i_p0_v
    subgraph "$.ip"
    is1_i_p0_v[Selector]
    is1_i_p0_v -->|scope| is1_i_p0_v_s[RootSelectorScope]
    is1_i_p0_v -->|fragment0| is1_i_p0_v_f0[PropertySelectorFragment] -->|key| is1_i_p0_v_f0_k([ ip ])
    end

    is1_i -->|property1| is1_i_p1[ObjectProperty]
    is1_i_p1 -->|key| is1_i_p1_k([ ])
    is1_i_p1 -->|value| is1_i_p1_v
    subgraph "$.ports[].port"
    is1_i_p1_v[Selector]
    is1_i_p1_v -->|scope| is1_i_p1_v_s[RootSelectorScope]
    is1_i_p1_v -->|fragment0| is1_i_p1_v_f0[PropertySelectorFragment] -->|key| is1_i_p1_v_f0_k([ ports ])
    is1_i_p1_v -->|fragment1| is1_i_p1_v_f1[IndexSelectorFragment] -->|indexes| is1_i_p1_v_f1_i([ ])
    is1_i_p1_v -->|fragment2| is1_i_p1_v_f2[PropertySelectorFragment] -->|key| is1_i_p1_v_f2_k([ port ])
    end
    end

```
## Cartesian product
Input:
```json
{
  "colors": [
    "red",
    "blue"
  ],
  "cars": [
    "Ford",
    "BMW"
  ]
}
```
Output:
```csv
"color","car"
"red","Ford"
"red","BMW"
"blue","Ford"
"blue","BMW"
```

Oui program:
```
{ color: $.colors[], car: $.cars[] }
```
Model:
```mermaid
flowchart
    Program -->|first| is0[InstructionSet] -->|instruction| is0_i
    subgraph "{ color: $.colors[], car: $.cars[] }"
    is0_i[Object]

    is0_i -->|property0| is0_i_p0[ObjectProperty]
    is0_i_p0 -->|key| is0_i_p0_k([ color ])
    is0_i_p0 -->|value| is0_i_p0_v
    subgraph "$.colors[]"
    is0_i_p0_v[Selector]
    is0_i_p0_v -->|scope| is0_i_p0_v_s[RootSelectorScope]
    is0_i_p0_v -->|fragment0| is0_i_p0_v_f0[PropertySelectorFragment] -->|key| is0_i_p0_v_f0_k([ colors ])
    is0_i_p0_v -->|fragment1| is0_i_p0_v_f1[IndexSelectorFragment] -->|indexes| is0_i_p0_v_f1_i([ ])
    end

    is0_i -->|property1| is0_i_p1[ObjectProperty]
    is0_i_p1 -->|key| is0_i_p1_k([ car ])
    is0_i_p1 -->|value| is0_i_p1_v
    subgraph "$.cars[]"
    is0_i_p1_v[Selector]
    is0_i_p1_v -->|scope| is0_i_p1_v_s[RootSelectorScope]
    is0_i_p1_v -->|fragment0| is0_i_p1_v_f0[PropertySelectorFragment] -->|key| is0_i_p1_v_f0_k([ cars ])
    is0_i_p1_v -->|fragment1| is0_i_p1_v_f1[IndexSelectorFragment] -->|indexes| is0_i_p1_v_f1_i([ ])
    end
    end

```

## Matrix extraction
Input:
```json
[
  [ {"x": 0, "y": 0}, {"x": 1, "y": 0}, {"x": 2, "y": 0} ],
  [ {"x": 0, "y": 1}, {"x": 1, "y": 1}, {"x": 2, "y": 1} ],
  [ {"x": 0, "y": 2}, {"x": 1, "y": 2}, {"x": 2, "y": 2} ]
]

```
Output:
```csv
"x","y"
0,0
1,0
2,0
0,1
1,1
1,1
0,2
1,2
2,2
```

Oui program:
```
$[][]
```
Model:
```mermaid
flowchart
    Program -->|first| is0[InstructionSet] -->|instruction| is0_i
    subgraph "$[][]"
    is0_i[Selector]
    is0_i -->|scope| is0_i_s[RootSelectorScope]
    is0_i -->|fragment0| is0_i_f0[IndexSelectorFragment] -->|indexes| is0_i_f0_i([ ])
    is0_i -->|fragment1| is0_i_f1[IndexSelectorFragment] -->|indexes| is0_i_f1_i([ ])
    end

```

## Large program
Input:
```json
{
  "quiz": {
    "sport": {
      "q1": {
        "question": "Which one is correct team name in NBA?",
        "options": [
          "New York Bulls",
          "Los Angeles Kings",
          "Golden State Warriros",
          "Huston Rocket"
        ],
        "answer": "Huston Rocket"
      }
    }
  }
}
```
Output:
```csv
"id","category","question"
"q1","sport","Which one is correct team name in NBA?"
```

Oui program:
```
$.quiz | entries | category=$.key; $.value | entries | {id: $.key, $category, $.value.question}
```
Model:
```mermaid
flowchart
    Program -->|first| is0[InstructionSet] -->|instruction| is0_i
    
    subgraph "$.quiz"
    is0_i[Selector]
    is0_i -->|scope| is0_i_s[RootSelectorScope]
    is0_i -->|fragment0| is0_i_f0[PropertySelectorFragment] -->|key| is0_i_f0_k([ quiz ])
    end

    is0 -->|next| is1[InstructionSet] -->|instruction| is1_i
    subgraph "entries"
    is1_i[Selector]
    is1_i -->|scope| is1_i_s[FilterSelectorScope] -->|filter| is0_i_s_f([ entries ])
    end

    is1 -->|next| is2[InstructionSet]
    
    is2 -->|declaration0| is2_d0[Declaration] -->|name| is2_d0_n([ category ])
    is2_d0 -->|value| is2_d0_v
    is4_i_p1_v_s -.->|declaration| is2_d0
    subgraph "$.key"
    is2_d0_v[Selector]
    is2_d0_v -->|scope| is2_d0_v_s[RootSelectorScope]
    is2_d0_v -->|fragment0| is2_d0_v_f0[PropertySelectorFragment] -->|key| is2_d0_v_f0_k([ key ])
    end
    
    is2 -->|instruction| is2_i
    subgraph "$.value"
    is2_i[Selector]
    is2_i -->|scope| is2_i_s[RootSelectorScope]
    is2_i -->|fragment0| is2_i_f0[PropertySelectorFragment] -->|key| is2_i_f0_k([ value ])
    end

    is2 -->|next| is3[InstructionSet] -->|instruction| is3_i
    subgraph entries2 [entries]
    is3_i[Selector]
    is3_i -->|scope| is3_i_s[FilterSelectorScope] -->|filter| is3_i_s_f([ entries ])
    end

    is3 -->|next| is4[InstructionSet] -->|instruction| is4_i
    subgraph "{id: $.key, $category, $.value.question}"
    is4_i[Object]

    is4_i -->|property0| is4_i_p0[ObjectProperty]
    is4_i_p0 -->|key| is4_i_p0_k([ id ])
    is4_i_p0 -->|value| is4_i_p0_v
    subgraph key2 [$.key]
    is4_i_p0_v[Selector]
    is4_i_p0_v -->|scope| is4_i_p0_v_s[RootSelectorScope]
    is4_i_p0_v -->|fragment0| is4_i_p0_v_f0[PropertySelectorFragment] -->|key| is4_i_p0_v_f0_k([ key ])
    end

    is4_i -->|property1| is4_i_p1[ObjectProperty]
    is4_i_p1 -->|key| is4_i_p1_k([ ])
    is4_i_p1 -->|value| is4_i_p1_v
    subgraph "$category"
    is4_i_p1_v[Selector]
    is4_i_p1_v -->|scope| is4_i_p1_v_s[DeclarationSelectorScope] 
    end

    is4_i -->|property0| is4_i_p2[ObjectProperty]
    is4_i_p2 -->|key| is4_i_p2_k([ ])
    is4_i_p2 -->|value| is4_i_p2_v
    subgraph "$.value.question"
    is4_i_p2_v[Selector]
    is4_i_p2_v -->|scope| is4_i_p2_v_s[RootSelectorScope]
    is4_i_p2_v -->|fragment0| is4_i_p2_v_f0[PropertySelectorFragment] -->|key| is4_i_p2_v_f0_k([ value ])
    is4_i_p2_v -->|fragment1| is4_i_p2_v_f1[PropertySelectorFragment] -->|key| is4_i_p2_v_f1_k([ question ])
    end
    end

```
