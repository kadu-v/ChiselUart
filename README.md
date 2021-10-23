# ChiselUart

## How to generate Verilog code
```
$ sbt run Elaborate --target-dir out
```

## How to generate Vcd file of test
```
$ testOnly uart.UartRxSpec -- -DwriteVcd=1
```