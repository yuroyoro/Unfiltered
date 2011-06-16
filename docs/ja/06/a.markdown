Requestマッチャ
----------------

### MethodとPath

Unfilteredは *request matcher* として、HTTPメソッドやヘッダーに対する様々な抽出子(extractor)オブジェクトを提供してる。 アプリケーションはリクエストを処理するかどうか、どのように処理するかをrequest matcherを使って定義するんだ。

```scala
case GET(Path("/record/1")) => ...
```
この場合は、 `/record/1` というパスに対するGETリクエストにマッチする。パスに対するマッチでは、追加で他の抽出子をネストさせることが可能だ。

```scala
case GET(Path(Seg("record" :: id :: Nil))) => ...
```
これは、 `record`の下にあるなんらかの文字列が `id` にマッチする。 `Seq` 抽出子は、通常は `Path` マッチャにネストされる形で、文字列に対してのマッチを行う。 `Seq` は与えられたスラッシュで区切られている文字列を、文字列のリストに分解する。

### Request読み込みと遅延マッチ

上に挙げたcase節は `record`に対するGETリクエストにマッチした。じゃあ、PUTだったらどうするかって?

```scala
case req @ PUT(Path(Seg("record" :: id :: Nil))) =>
  val bytes = Body.bytes(req)
  ...
```

<!--
Access to the request body generally has side effects, such as the
consumption of a stream that can only be read once. For this reason
the body is not accessed from a request matcher, which could be
evaluated more than one time, but from utility functions that operate
on the request object.
-->

リクエストボディへのアクセスは、一度しか読めないストリームを消費するって意味で、副作用を持っている。そんなワケで、リクエストマッチャからはbodyにはアクセスできないし、評価できるのは一度きりだが、リクエストオブジェクトへのユーティリティ関数を使うことができる。

<!--
In this case, we assigned a reference to the request using `req @` and
then read its body into a byte array—on the assumption that its body
will fit into available memory. That aside, a minor annoyance is that
this code introduces some repetition in the matching expression.
-->

こんな風に、リクエストへの参照を `req @` で取得して、(メモリ上にすべて乗ると仮定して)バイト配列を読み出している。ところで、マッチ式で同じ表現が繰り返されているのがちょっとイラッとくるよな。

```scala
case GET(Path(Seg("record" :: id :: Nil))) => ...
case req @ PUT(Path(Seg("record" :: id :: Nil))) => ...
```
代わりに、さっきのメソッドで、まず最初にパスにマッチさせる。

```scala
case req @ Path(Seg("record" :: id :: Nil)) => req match {
  case GET(_) => ...
  case PUT(_) => ...
  case _ => ...
}
```
<!--
This approach eliminates the duplicated code, but it's important to
recognize that it behaves differently as well. The original intent
partial function was simply *not defined* for request to that path
that were not a GET or a PUT. The latest one matches any request to
that path, and therefore it must return a value for all methods with
its match expression.
-->

このアプローチで重複したコードを消し去ることができたけど、重要なのは異なる振る舞いをするってことだ。元のintentでのpartial-functionは、単にそのパスへのGET/PUTリクエスト以外は *定義されていない* だけだった。でも、新しいやつは、パスにリクエストがマッチした場合、全てのHTTPメソッドに対してなんらかの値を返さなきゃならないんだ。

<!--
Importantly, delaying the match on request method simplified the
intent partial function. What used to be two cases is now one, and we
could add support for other methods like DELETE without adding any
complexity to its pattern matching. This is worth noting because
`ifDefined` is called on every intent at least once prior to its
evaluation. By making the intent more broadly defined, we've reduced
the complexity of that and potentially improved runtime performance.
-->

大事なことなので2回言うけど、リクエストメソッドのマッチを遅らせると、intentをシンプルになる。
ふたつのcase節がひとつになって、複雑さを増すことなくDELETEのような他のメソッドをサポートできるようになった。
`ifDefined` はintent毎にすくなくとも一回は呼び出されるってことは注意しておかなきゃならない。
ともあれ、intentをより広く定義することで、複雑さを減少させてランタイム性能が潜在的に向上するんだ。


