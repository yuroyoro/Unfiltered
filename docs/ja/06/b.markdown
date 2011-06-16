Response関数
------------------

### Response Function と Combinators

<!--
With a typical request-response cycle intent, the partial function's
return value is of Unfiltered's type `ResponseFunction`. A response
function takes a response object, presumably mutates it, and returns
the same response object.
-->

典型的なリクエスト/レスポンスサイクルのintentでは、partial-functionの返り値はUnfilteredの `ResponseFunction` 型だ。
レスポンス関数はresponseオブジェクトを引数にとって、おそらくその状態を変更してそのまま同じレスポンスオブジェクトを戻す。

<!--
Unfiltered includes a number of response functions for common response
types. Continuing the "record" example, in some cases we may want to
respond with a particular string:
-->

Unfilteredは共通のレスポンス型に対して多数のレスポンス関数を用意してる。"record"の例で続けると、しばしば特定の文字列をレスポンスとして返したいことがあるかもしれない。

```scala
  case PUT(_) =>
    ...
    ResponseString("Record created")
```

<!--
We should also set a status code for this response. Fortunately there
is a predefined function for this too, and response functions are
easily composed. Unfiltered even supplies a chaining combinator `~>`
to make it pretty:
-->

また、レスポンスにはステータスコードも設定すべきだし。幸運にも、そういった関数もあらかじめ用意されていて、レスポンス関数は簡単に合成できるって。
そのために、Unfilteredは、合成のためのステキなコンビネータとして `~>` を提供する。

```scala
  case PUT(_) =>
    ...
    Created ~> ResponseString("Record created")
```

<!--
If we had some bytes, they would be as easy to serve as strings:
-->

なんらかのbyte列を取得できたら、このように簡単に文字列を返せるんだ。

```scala
  case GET(_) =>
    ...
    ResponseBytes(bytes)
```

Passing or Handling Errors
--------------------------

<!--
And finally, for the case of unexpected methods we have a few
choices. One option is to *pass* on the request:
-->

それじゃあ最後に、期待しないHTTPメソッドの場合についてだ。ひとつの選択肢として、 requestを *Pass* するって手がある。

```scala
  case _ => Pass
```

<!--
The `Pass` response function is a signal for the plan act as if the
request was not defined for this intent. If no other plan responds to
the request, the server may respond with a 404 eror. But we can
improve on that by ensuring that any request to this path that is not
an expected method receives an appropriate response:
-->

`Pass` レスポンス関数は、リクエストへの振る舞いが未定義であることを伝えるシグナルだ。もし他のplamもリクエストに応答しなければ、サーバーは404エラーを返すだろうね。だけど、このパスのどんなリクエストに対しても、期待しないHTTPメソッドだったら適切なレスポンスを返すように改良することができる。


```scala
  case _ => MethodNotAllowed ~> ResponseString("Must be GET or PUT")
```
