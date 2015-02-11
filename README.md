## WAT?

A custom advanced user data provider for Netflix/Asgard.

## Run?

```bash
export ZK_CONN_STRING=wherever:whatever
export ZK_AES_KEY=Wh3R3v3RwH473V3r

./run
```

* `ZK_CONN_STRING` is optional, will use `localhost:2181` if not present
* `ZK_AES_KEY` is mandatory, it'll be used to decrypt data from ZooKeeper.
