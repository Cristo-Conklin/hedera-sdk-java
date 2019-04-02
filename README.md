# Hedera Java SDK

[![CircleCI](https://circleci.com/gh/swirlds/hedera-sdk-java.svg?style=shield&circle-token=d288d5d093d2529ad8fbd5d2e8b3e26be22dcaf7)](https://circleci.com/gh/swirlds/hedera-sdk-java)
[![Discord](https://img.shields.io/discord/373889138199494658.svg)](https://hashgraph.com/discord/)

The Java SDK for interacting with [Hedera Hashgraph](https://hedera.com): the official distributed consensus platform built using the hashgraph consensus algorithm for fast, fair and secure transactions. Hedera enables and empowers developers to build an entirely new class of decentralized applications.

# Note about this branch
This is work in progress in preparation for a testnet update. The branch of the SDK **WILL NOT WORK** on public testnets. Please use branch 0.2.0 or 0.2.2 unless you have been advised to use this one.
Also, for this SDK version to compile, you will need to download and `mvn install` the `vNext` branch of the hedera-protobuf project.

## Version is 0.3.0

The Hedera Java SDK uses [semantic versioning](https://semver.org/).

Features supported include:

- Micro-payments between two Accounts
- Storing files to Hedera
- Creating [Solidity](https://solidity.readthedocs.io/en/latest/index.html) Smart Contracts
- Executing Smart Contracts

## Changes in this version from 0.1.0

### 0.3.0 - Added expiry to claims
Claims now support an expiry expressed a duration in seconds from the time of creation

### 0.3.0 - HederaDuration class has static constructors for common durations

Standard durations for minute (60 sec), hour (60 minutes), day (24 hours), week (7 days), month (30 days) and year (365 days)

For example:

`public HederaDuration duration = HederaDuration.HederaDurationYear();`

### 0.3.0 - Added smart contract memo support

Smart contracts can now have a memo attached on create and it may be updated.

### 0.3.0 - Removal of deprecated signature related entities

Signature, SignatureList and ThresholdSignature

### 0.3.0 - Implementation of new signatures

`SignatureMap` and `SignaturePair` are now used to build signatures for transactions and queries.
Signatures are now supplied to Hedera as a public key + signature pair. The public key can be reduced to just enough data to make it uniquely identifiable.
`HederaSignatures` implements the necessary logic to reduce public keys to the minimum necessary.

### 0.3.0 - Examples - getRecords Queries

Added getRecords examples to account and smart contract.
Note: The simpleStorage contract now emits an event when the `set()` function is called, this generates a record which can be retrieved with the `getRecords` query.

### 0.3.0 - SDK - Account update bug fixes

Bug fixes to the `HederaAccount.update` code

### 0.3.0 - SDK - Added support for ContractGetRecordsQuery

Returns records related to a smart contract that were created within the last 24h, events emitted by a smart contract are stored in such records

### 0.3.0 - SDK - Added methods to HederaContract class

`getHederaContractID()` and `setHederaContractID()` were missing, now added.

### 0.3.0 - Examples - change to contract .bin file loading

the previous version of ExampleUtilities.java could result in the IDE loading the required file from a number of possible locations (e.g. `/src/main/resources`, `./target/classes`), the `readFile` method of this class now loads files from a path relative to the root of the project (e.g. `./src/main/resources/scExamples/HelloWorld.bin`).

### 0.2.4 - Bug fix to EDKeypair.java

Private key was being incorrectly returned

### 0.2.4 - Simplified smart contract call wrappers

see com.hedera.examples.contractWrappers
added `ContractFunctionsWrapper.java` which wraps all helpers related to making calls to smart contracts.

Put your full ABI into a string (or a file with a path relative to the project).

Initialise a `ContractFunctionsWrapper` object as follows (fullABI is a string containing my smart contract ABI in JSON format).

```java
ContractFunctionsWrapper wrapper = new ContractFunctionsWrapper();
		wrapper.setABI(fullABI);
```

Locally call a function named `getInt` on the smart contract which returns a `BigInteger`.

```java
BigInteger decodeResult = wrapper.callLocalBigInt(createdContract, localGas, maxResultSize, "getInt");
```

Locally call a function named `getString` on the smart contract which returns a `String`.

```java
String decodeResult2 = wrapper.callLocalString(createdContract, localGas, maxResultSize, "getString");
```

There are overloaded methods for `Int`, `String`, `boolean`, `long`, `address` and `BigInt`.

or, call a function named `set`

```java
wrapper.call(contract, gas, amount, "set", 10);
```

### 0.2.3 - Changes to pom structure

Changes to pom structure including removal of parent project and inclusion of dependency version numbers.

### 0.2.3 - Changes to protobuf repo version

Changed dependency on protobuf repo from 0.2.0 to 0.2.1

### 0.2.2 - Switch to Hedera Hashgraph Protobuf repo

This version uses a new repository to fetch the protobuf API from to better control version management.

### 0.2.1 - Change to artifact IDs

The maven dependency for the sdk is now

```maven
<dependency>
  <groupId>com.hedera.hashgraph</groupId>
  <artifactId>hedera-java-sdk</artifactId>
  <version>0.2.1</version>
</dependency>
```

### 0.2.1 - CallLocal exception

Calling a smart contract function locally now raises an exception if the result from Hedera doesn't include a response result. This usually indicates that either the smart contract didn't initialise properly (lack of gas), or the function call itself wasn't supplied enough gas to execute.

### 0.2.1 - Javadoc

Javadocs are generated during `mvn install` and are located in `sdk/javadoc` as html files rather than a .jar in `sdk/target/` previously

### 0.2.1 - Additional smart contract examples

- Extra smart contract examples (HelloWorld, Simple Storage and Token)
- Documentation with smart contract examples (.sol source, compilation information, ABI in examples/src/main/resources)

### 0.2.1 - ExampleUtilities.readFile

This loads a file from the file system. It incorporates a number of fixes from the previous version which could in some instances miss the last byte of a file content.

### 0.2.1 - ExampleUtilities.checkBinFile

This method checks the contents of a supplied byte array for non Hex characters. It will raise an error if the byte array contains characters other than 0-9, a-f and A-F. This is a useful check to perform prior to loading a compiled solidity file to ensure it doesn't contain unaccepted characters such as Line Feeds, Carriage Returns and others which are sometimes introduced by text editors upon saving.

This loads a file from the file system. It incorporates a number of fixes from the previous version which could in some instances miss the last byte of a file content.

### 0.2.1 - public and private keys

You may now use either encrypted public and private keys or their non-encrypted version.

### 0.2.1 - Overloaded methods for generating and recovering keypairs

The `HederaKeyPair` class has additional overloaded methods for generating keys at a given index and recovering keys from words at a given index. Default behaviour in unchanged (index=-1), these overloaded methods enable you to generate/recover keys that are compatible with the mobile Hedera wallet.

### 0.2.0 - Project structure

The SDK and examples are now two separate projects under the java-sdk project itself. This enables the SDK to be published to a maven repository without the examples included.
The examples also refer to the SDK as a maven dependency.

### 0.2.0 - SDK available as a maven dependency

You may now add the SDK to your project as a maven dependency as follows:

```maven
<dependency>
  <groupId>com.hedera.hashgraph</groupId>
  <artifactId>hedera-java-sdk</artifactId>
  <version>0.2.1</version>
</dependency>
```

This is the link to the maven repository : [hedera java sdk on maven](https://search.maven.org/artifact/com.hedera.hashgraph/java-sdk/0.2.0/jar)

### 0.2.0 - Examples have been simplified

Examples no longer create unnecessary accounts or transfer unnecessary large amounts of tinybar to newly created accounts. Likewise, the gas specified for running the smart contract examples are the bare minimum required.

### 0.2.0 - Logging framework

SLF4J has been switched for Logback to help compatibility with Swing projects amongst others

### 0.2.0 - HederaCryptoKeyPair and HederaKey Classes

These classes have been deprecated, the `HederaKeyPair` class should be used instead.

```java
HederaKeyPair ed25519Key = new HederaKeyPair(KeyType.ED25519);
```

will create a new private/public key pair

```java
HederaKeyPair payingKeyPair = new HederaKeyPair(KeyType.ED25519, pubKey, privKey);
```

will create a key pair object from an existing keypair.

__Note__: *if you only have a public key and no private key, set the privKey parameter to null or "")*

### 0.2.0 - SigsForTransaction parameter

The `sigsForTransaction` parameter on methods of the `HederaAccount`, `HederaContract` and `HederaFile` classes has changed from `HederaKeySignatureList` to `HederaSignatureList`.

### 0.2.0 - Transaction fees

Transaction fees are 100,000 *TinyBars*. Testnets now implement charging for transactions and queries, so your testnet account balance will reduce with each request.

### 0.2.0 - Improved error responses from Hedera

Hedera now reports a greater number of error codes in response to queries and transactions. As a result, `HederaPrecheckResult` and `HederaTransactionStatus` classes have been removed and replaced by `com.hederahasgraph.api.proto.java.ResponseCodeEnum`.

### 0.2.0 - Trace logging removed

All calls to `logger.trace` have been removed from the SDK.

## Environment Set-up

### Pre-requisites

- [JDK](https://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html): 10.0.x
- [Maven](https://maven.apache.org/): 3.6.0

   To check the versions of these products from CLI use:

   Java: `java -version`

   Maven: `mvn --version`

### Installing

Maven Installation

- follow the instructions here: https://maven.apache.org/install.html to install maven version 3.6.0
- once complete, confirm that `mvn -v` in a terminal/command line returns version 3.6.0.

From [Eclipse](https://www.eclipse.org/downloads/) or [IntelliJ](https://www.jetbrains.com/idea/):

- Right click on `pom.xml` under the `java-sdk` project

- `Run As` -> `Maven Install`

From CLI:

```shell
$> mvn install
```

If there are still some project issues, try a Maven project update and project clean followed by a Maven install.

### Errors and solutions

```
Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project java-sdk: Fatal error compiling: invalid target release: 10 -> [Help 1]
```

- Check the version of the JRE in use by the project and change it to 10.

## Running the examples

A `node.properties.sample` is provided, copy the file to `node.properties` and update with your account details, the details of the node you want to communicate to, and your private/public keys (as hex strings).

   This file is ignored by git so all changes will remain local.

The example project contains three packages that are "wrappers" to transactions.
A further `com.hedera.examples.simple` package contains three main classes `DemoAccount.java`, `DemoContract.java` and `DemoFile.java`, these run the examples and use the wrappers to invoke the SDK, check for responses, etc...

The `DemoAsync.java` class is an example for making calls to the Hedera network and asynchronously (separate thread) deal with the responses. This is by no means complete, but a starter for an asynchronous implementation that could involve callbacks, etc...

## Javadocs

Javadocs are generated automatically as part of the Maven build.

They are generated under the sdk/javadoc folder.

## Building a simple test with the Hedera SDK for Java

Code snippets used in this document are excerpt from the full examples included in this repo. 

## Prerequisites for using the SDK

### Access to the Hedera mainnet or a Hedera testnet

In order to be able to use the Hedera SDK for Java you must have access to a testnet or to the Hedera mainnet. Access to mainnet and testnets are currently restricted.

Temporary testnets (or "Flashnets") may become available for specific events and engagements such as hackathons.

To get access to any Hedera network you must first create a Hedera Profile in the [Hedera Portal](https://portal.hedera.com), then enter an access or promo code that was received from the Hedera team.

#### Addresses of testnet nodes

After you join a network through the Hedera Portal, you will be provided information required connect to that network, specifically:

- __IP address__ and/or __DNS name__ – providing IP connectivity to that node
- __Port number__ – The specific port on the node to which queries and transactions must be sent
- __Account ID__ – the Hedera account number associated with the node. This is required in order to issue transactions and queries. It determines the account to which node-fees will be paid. Hedera account IDs are made up of three int64 numbers separated by colons (e.g. 0:0:3) The three numbers represent __Shard-number__, __Realm-number__ and __Account-number__ respectively. Shards and Realms are not yet in use, so you should expect Account IDs to start with two zeros for the present time.

__NOTE__: *This information should be added to your `node.properties` file (see above) if you wish to run the example code provided.*

#### Public/Private Key Pairs

Hedera accounts must be associated with cryptographic keys. ED25519 key pairs can be generated using the [Hedera Keygen utility](https://github.com/hashgraph/hedera-keygen-java). A complete explanation of the key generation process is documented in the [readme](https://github.com/hashgraph/hedera-keygen-java/blob/master/README.md) file in that repo. A minimal version of that process will be shown below.

__NOTE__: *This information should be added to your `node.properties` file (see above) if you wish to run the example code provided.*

### Using the Hedera SDK for Java

The following steps describe some first steps using the SDK:

1. Generate a Key Pair
2. Create a new java project
3. Create a Hedera account
4. Retrieve the balance of a Hedera account
5. Transfer hbars between Hedera accounts

#### Generating a Key Pair
Download the [sdk-keygen-jar](https://github.com/hashgraph/hedera-keygen-java/blob/master/target/sdk-keygen-1.0.jar) to a folder in your development environment.

Open that folder using terminal and execute the following command to generate a public/private key pair based on a system-generated random seed:

```Shell
java -jar sdk-keygen-1.0.jar
```

##### Example Output

```text
Your key pair is:
Public key:
302a300506032b6570032100a1a16c812bdc3b260d3f7b42c33b8f80337fbfd3ac0df703015b096b55e99d9f
Secret key:
302e020100300506032b657004220420975b637b1f648ae04e7e6109542f57cb58667c50e2366b91e76199bd458e2620
Recovery word list:
[ink, enable, opaque, clap, make, toe, brine, tundra, cater, Joe, small, run, Seoul, grand, atom, crush, circus, abbey, vacuum, whim, hollow, afar]
```

Copy this information into a safe place as you will need these keys below.

Should you wish to specify your own seed, please refer to the Hedera KeyGen [readme](https://github.com/hashgraph/hedera-keygen-java/blob/master/README.md) file.

#### Create a new Java project

Open your IDE of choice (Eclipse, IntelliJ, VSCode...)

Refer to the java sdk in your maven pom file as follows:

```maven
<dependency>
  <groupId>com.hedera.hashgraph</groupId>
  <artifactId>hedera-java-sdk</artifactId>
  <version>0.3.0</version>
</dependency>
```

Once you have added that dependency to your project and completed a maven install, you should be able to import classes from the SDK for use within your application.

#### Create a Hedera account

In the interest of clarity, these examples assume a sunny-day scenario and do not include exception-handling logic. Refer to the examples for more resilient code.

This is the definition of the simple version of the `create` method in the `HederaAccount` class:

```java
public HederaTransactionResult create(long shardNum,
                                      long realmNum,
                                      String publicKey,
                                      KeyType keyType,
                                      long initialBalance,
                                      HederaAccountCreateDefaults defaults
                                      )
                                      throws InterruptedException
```

The purpose of each of the parameters is as follows:

`shardNum` - the shard number for the new account. Note that this is not currently used, and should be set to 0 at present.

`realmNum` - the realm number for the new account. Note that this is not currently used, and should be set to 0 at present.

`publicKey` - the public key for the new account. This should be set to the __Public Key__ generated using the Hedera Key Generation Tool (see above).

`keyType` - The type of cryptographic key used by this account. In future, a variety of standards will be supported; at present only ED25519 keys are supported.

`initialBalance` - A Hedera account must contain *hbars*  on creation. This parameter describes that opening balance in *TinyBars*.
__Note__:  100,000,000 *TinyBars* is equivalent to 1 *hbar*.

`defaults` - The Hedera SDK for Java makes extensive use of defaults parameters to maximise reuse and readability. These defaults help new developers to get started without the need to understand all of the necessary parameters in detail. Once you are familiar with basic functionality of each method, additional behaviour can be unlocked by modifying these defaults.

##### Using HederaAccount.create

Utility functions have been provided within the examples. The first of these that we should use configures default settings for all transactions and queries.

__Note__: *For these example steps to function as expected, you must have updated the `node.properties` file as described above. The `pubkey` + `privkey` and `payingAccount...` parameters are used to determine the account from which *hbars* are transferred.*

In order to create a Hedera account, an initial balance must be transferred into the new account from an existing account. The `exampleUtilities.java` package retrieves details of the "source" or paying account from the `node.properties` file.

A `HederaAccount` variable must be defined and associated with the `txQueryDefaults` we just created.

```java
HederaAccount newAccount = new HederaAccount();

// setup transaction/query defaults (durations, etc...)
newAccount.txQueryDefaults = ExampleUtilities.getTxQueryDefaults();
```

To keep things simple in this example, a cryptographic record of the transaction is not required. In the following code-snippet, a new cryptographic private/public key pair is generated for the new account, specifying a `KeyType` of ED25519. This is equivalent to generating a key pair using the Hedera Key Generation utility. It is worth making a note of those public and private keys.

```java
newAccount.txQueryDefaults.generateRecord = false;
HederaKeyPair newAccountKey = new HederaKeyPair(KeyType.ED25519);
```

Now that everything is set up correctly, the following statement should create a Hedera account by transferring 100,000 *TinyBars* from the paying account defined in `node.properties` into the new account.

```java
newAccount = AccountCreate.create(newAccount, newAccountKey.getPublicKeyHex(), newAccountKey.getKeyType(), 100000);
```

#### Retrieve the balance of a Hedera account
Assuming that you have completed the steps above, the following statement will retrieve the balance of the account by querying the network.

```java
long balance1 = newAccount.getBalance();
```

#### Transfer *hbars* between Hedera accounts
To transfer *hbars* from your account to the newly created account above, we need to create an object to hold your account details.

```java
HederaAccount myAccount = new HederaAccount();

// setup transaction/query defaults (durations, etc...)
myAccount.txQueryDefaults = ExampleUtilities.getTxQueryDefaults();
// sets the accountNum property of myAccount
myAccount.accountNum = myAccount.txQueryDefaults.payingAccountID.accountNum;
```

At this stage, two accounts: `newAccount` and `myAccount` exist as objects, newAccount was created on the Hedera Network, `myAccount` already exists, there is no need to create it.

In the supplied examples, the `txQueryDefaults` object contains details of the original paying account used to fund the opening of both accounts; these defaults were read from the `node.properties` file.

To send the transfer transaction to Hedera, transferring 10,000 *TinyBars* from `myAccount` to `newAccount` the following code should be used:

```java
AccountSend.send(myAccount, newAccount, 10000);
```

To verify that `myAccount` now contains fewer *TinyBars* and `newAccount` contains more *TinyBars* the following instructions should suffice.

```java
long balance1 = myAccount.getBalance();
long balance2 = newAccount.getBalance();
```

__Note__: *In the event you're not waiting for consensus on the transfer transaction, it's possible that the balances will initially show no change. Adding a small delay between the transfer and the balance queries will ensure the correct values are returned. Ideally, you would ask for a receipt and check transaction status following the transfer transaction before querying the updated balances. This is shown in the examples contained within the SDK.*

## Smart contracts

### Good to know

- `msg.sender` in Solidity refers to the Hedera Account which issues the transaction.
- `address` an address value in a solidity function will accept a `HederaContract.getSolidityContractAccountID` or `HederaAccount.getSolidityContractAccountID`. These are populated following a getInfo on either a `HederaContract` or `HederaAccount` object.

### Auto-renew duration

The examples set the auto-renew duration of a newly created smart contract to 60s. This is to avoid paying unnecessary smart contract storage fees while testing. As a result, unless the smart contract's account is funded, the smart contract will cease to exist 60 seconds after being created (note, a file is valid for 1 day by default).

If you refer to a smart contract created earlier than 60s ago in a subsequent call, it may no longer be available, be sure to increase the default duration and fund the smart contracts' account if necessary.

### Common pitfalls

#### Invalid characters in bin file

Some text editors add a CR/LF at the end of a file when it is saved, this will result in an internal error in Hedera and your smart contract will fail to load. Please ensure the file only contains valid hex (0-9, a-f and A-F).

#### Gas

It is impossible for Hedera to predict how much gas will be necessary for any operation. In the Hedera context, gas is used to specify the maximum amount you're prepared to spend and won't affect the execution order of your transaction.

- Hedera will first check your account balance and ensure there are enough funds to pay for the transaction and the gas you specify (gas price is 1:1 with tinybar for now). In the event there aren't sufficient funds in the account, the transaction will fail with `INSUFFICIENT_PAYER_BALANCE`.

- Specifying high gas values. Hedera will only charge the amount of gas used and not the full amount if less was necessary. Furthermore, higher gas values do not affect the order or acceptance of a transaction.

- Insufficient gas supplied for creation. In this instance, the smart contract will fail to create, but the gas used to reach that conclusion will be charged to the account. The error returned in this instance is `INSUFFICIENT_GAS`.

- How much gas did a create or function call cost ? Getting a record following the smart contract creation will indicate how much gas was consumed during execution.

- My function call doesn't return the expected result. This may be due to insufficient gas being supplied. In the case of a local query, the node doesn't respond with an `INSUFFICIENT_GAS` error, this is a bug which is under investigation.

__Note__: *Hedera sometimes reports a smart contract creation being successful (it looks that way), when it has in fact failed. A smart contract reference is returned in the receipt, but the smart contract itself has failed to start. This is often due to a lack of gas (although the transaction didn't report `INSUFFICIENT_GAS`) and will manifest itself when you attempt to call functions on the smart contract. If this happens, try to increase the gas until a readonly function works, this will confirm the smart contract created itself fully.*

#### Compiler version

All examples were compiled with Remix using compiler version `0.5.3+commit.10d17f24.Emscripten.clang`

#### Initial balance parameter for create

This parameter is only applicable to smart contracts with payable constructors, set the value to 0 in all other cases.

## More information

To learn more about Hedera visit [The Hedera Site](https://hedera.com).

If you want to contribute please review the [Contributing Guide](https://github.com/hashgraph/hedera-sdk-java/blob/master/CONTRIBUTING.md).

#### Need help?

- Ask questions in [Discord](https://hashgraph.com/discord)
- Open a ticket in GitHub [issue tracker](https://github.com/hashgraph/hedera-sdk-java/issues)

#### License

Copyright (c) 2018-present, Hedera Hashgraph LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.