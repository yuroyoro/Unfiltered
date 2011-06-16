Silly Store
-----------

### Opening the Store

<!--
Using the request matchers and response functions outlined over the
last couple of pages, we have everything we need to build a naive
key-value store.
-->

これまでのページで、リクエストマッチャとレスポンス関数の使い方について概要を述べたよな。これでもう、ネイティブなkey-valueストアを作る準備はできてるってこった。

```scala
import unfiltered.request._
import unfiltered.response._

object SillyStore extends unfiltered.filter.Plan {
  @volatile private var store = Map.empty[String, Array[Byte]]
  def intent = {
    case req @ Path(Seg("record" :: id :: Nil)) => req match {
      case GET(_) =>
        store.get(id).map(ResponseBytes).getOrElse {
          NotFound ~> ResponseString("No record: " + id)
        }
      case PUT(_) =>
        SillyStore.synchronized {
          store = store + (id -> Body.bytes(req))
        }
        Created ~> ResponseString("Created record: " + id)
      case _ =>
        MethodNotAllowed ~> ResponseString("Must be GET or PUT")
    }
  }
}
```

<!--
Go ahead and paste that into a [console](Try+Unfiltered.html). Then,
execute the plan with a server, adjusting the port if your system does
not have 8080 available.
-->

先に進んで、これを[コンソール](Try+Unfiltered.html)に貼り付けて見てくれ。すると、planとともにサーバーが(もし使われていないなら)8080ポートで起動するハズだ。

```scala
unfiltered.jetty.Http.local(8080).filter(SillyStore).run()
```

<!--
The method `local`, like `anylocal`, binds only to the loopback
interface, for safety. SillyStore is not quite "web-scale".
-->

`local` メソッドは、 `anylocal` のように、安全のためループバックインターフェースにバインドされる。 SillyStoreは全然"web-scale"じゃないからね。

### Curling the Store

<!--
The command line utility [cURL][curl] is great for testing HTTP
servers. First, we'll try to retrieve a record.
-->

コマンドラインのユーティリティの [cURL][curl]はHTTPサーバーをテストするのに最適だ。まず、レコードを取り出してみよう。

[curl]: http://curl.haxx.se/

    curl -i http://127.0.0.1:8080/record/my+file

<!--
The `-i` tells it to print out the response headers. Curl does a GET
by default; since there is no record by that or any other name it
prints out the 404 response with our error message. We have to PUT
something into storage.
-->

`-i`オプションはレスポンスヘッダーを出力する。 CurlのデフォルトはGETだから、そもそもレコードが存在しないのでエラーメッセージとともに404エラーが表示されるだろう。まずはストレージに何かPUTする必要がある。

    echo "Ta daa" | curl -i http://127.0.0.1:8080/record/my+file -T -

<!--
Curl's option `-T` is for uploading files with a PUT, and the hyphen
tells it to read the data piped in from echo. Now, we should have
better luck with a GET request:
-->

Curlの `-T` オプションを使うとファイルをPUTでアップロードできる。で、ハイフンはechoコマンドからパイプされた内容を読み取る。さあ、GETリクエストの結果はもうちょっとマシになってるハズだ。

    curl -i http://127.0.0.1:8080/record/my+file

<!--
That worked, right? We should also be able to replace items:
-->

ちゃんと動いたかい? じゃあ、内容を置き換えてみるか。

    echo "Ta daa 2" | curl -i http://127.0.0.1:8080/record/my+file -T -
    curl -i http://127.0.0.1:8080/record/my+file

<!--
And lastly, test the method error message:
-->
最後は、エラーメッセージのテストだ。


    curl -i http://127.0.0.1:8080/record/my+file -X DELETE

<!--
405 Method Not Allowed. But it's a shame, really. DELETE support would
be easy to add. Why don't you give it a try?
-->

405 Method Not Allowed. でも、ガッカリする必要はない。なぜならDELETEをサポートするようにするのはとても簡単だからだ。やってみない手はないだろ?
