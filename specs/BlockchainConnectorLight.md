┌─────────────────────────────────────────────────────────────────┐
│   BLOCKCHAIN CONNECTOR MICRO KERNEL CORE *DRAFT*                │
├─────────────────────────────────────────────────────────────────┤
│  • Plugin Registry & Lifecycle Manager                          │
│  • Event Bus / Message Broker                                   │
│  • Configuration Manager                                        │
│  • Transaction Coordinator (Orchestrator)                       │
│  • Error Handling & Retry Logic                                 │
│  • Monitoring & Observability                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↕️
┌─────────────────────────────────────────────────────────────────┐
│                    PLUGIN INTERFACE LAYER                       │
│                                                                 │
│  AccountPlugin  |   NoncePlugin   |   GasPlugin   |  TxPlugin   │
└─────────────────────────────────────────────────────────────────┘
                              ↕️
┌──────────────────┬──────────────────┬───────────────────────────┐
│  ACCOUNT PLUGINS │   CORE PLUGINS   │   PROVIDER PLUGINS        │
├──────────────────┼──────────────────┼───────────────────────────┤
│                  │                  │                           │
│ • EOA Account    │ • Nonce Manager  │ • Alchemy Gas Station     │
│   Plugin         │   Plugin         │   Plugin                  │
│                  │                  │                           │
│ • Smart Account  │ • Transaction    │ • Alchemy AA Provider     │
│   Plugin (AA)    │   Manager Plugin │   Plugin                  │
│                  │                  │                           │
│ • Multi-Sig      │ • Gas Station    │ • Second Provider Plugin  │
│   Plugin         │   Coordinator    │                           │
│   (TBD)          │   Plugin         │ • Custom RPC Plugin       │
│                  │                  │                           │
└──────────────────┴──────────────────┴───────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                    BLOCKCHAIN CONNECTOR API                     │
│                      (REST & EVENT BUS)                         │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MICRO KERNEL (Core System)                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         BlockchainConnectorKernel                        │   │
│  │  - Plugin Discovery                                      │   │
│  │  - Transaction Orchestration                             │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                           │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │            PluginRegistry                                │   │
│  │  - Account Plugins Map                                   │   │
│  │  - Gas Plugins Map                                       │   │
│  │  - Nonce Plugins Map                                     │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                           │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │       TransactionCoordinator                             │   │
│  │  1. Reserve Nonce                                        │   │
│  │  2. Estimate Gas                                         │   │
│  │  3. Apply Gas Strategy                                   │   │
│  │  4. Prepare Transaction                                  │   │
│  │  5. Sign Transaction                                     │   │
│  │  6. Send Transaction                                     │   │
│  │  7. Confirm/Release Nonce                                │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────┬──────────────┬──────────────┬───────────────────┘
                │              │              │
       ┌────────▼────┐  ┌──────▼──────┐  ┌───▼──────────┐
       │   ACCOUNT   │  │     GAS     │  │    NONCE     │
       │   PLUGINS   │  │   PLUGINS   │  │   PLUGINS    │
       └─────────────┘  └─────────────┘  └──────────────┘
               │              │              │
    ┌──────────┼──────────┐   │   ┌──────────┼──────────┐
    │          │          │   │   │          │          │
┌───▼───┐ ┌────▼─────┐ ┌──▼───▼───▼──┐ ┌─────▼────┐ ┌───▼────┐
│  EOA  │ │ Alchemy  │ │   Alchemy   │ │  Redis   │ │Database│
│Plugin │ │  Smart   │ │Gas Station  │ │  Nonce   │ │ Nonce  │
│       │ │ Account  │ │   Plugin    │ │  Plugin  │ │ Plugin │
│       │ │  Plugin  │ │             │ │          │ │        │
└───┬───┘ └────┬─────┘ └──────┬──────┘ └────┬─────┘ └───┬────┘
    │          │              │             │           │
    │          │              │             │           │
    └──────────┼──────────────┼─────────────┼───────────┘
               │              │             │
        ┌──────▼──────────────▼──────────────▼──────┐
        │         External Dependencies             │
        │  - Web3j                                  │
        │  - Alchemy SDK                            │
        │  - Redis                                  │
        │  - Database (PostgreSQL)                  │
        └───────────────────────────────────────────┘


User Request (EOA) 
    ↓
BlockchainConnectorKernel
    ↓
PluginRegistry. getAccountPlugin(EOA) → EoaAccountPlugin
PluginRegistry.getGasPlugin(STANDARD) → StandardGasPlugin  
PluginRegistry.getNoncePlugin(EOA) → RedisNoncePlugin
    ↓
TransactionCoordinator
    ├─ 1. RedisNoncePlugin.reserveNonce()
    ├─ 2. StandardGasPlugin.estimateGas()
    ├─ 3. StandardGasPlugin.applyGasStrategy() (user pays)
    ├─ 4. EoaAccountPlugin. prepareTransaction()
    ├─ 5. EoaAccountPlugin.signTransaction() (RLP encoded)
    ├─ 6. EoaAccountPlugin.sendTransaction() (via Web3j)
    └─ 7. RedisNoncePlugin.confirmNonce()
    ↓
TransactionReceipt


---

User Request (Smart Account + Sponsored Gas)
    ↓
BlockchainConnectorKernel
    ↓
PluginRegistry.getAccountPlugin(SMART_ACCOUNT_ALCHEMY) 
    → AlchemySmartAccountPlugin
PluginRegistry.getGasPlugin(SPONSORED) 
    → AlchemyGasStationPlugin
PluginRegistry. getNoncePlugin(SMART_ACCOUNT_ALCHEMY) 
    → RedisNoncePlugin
    ↓
TransactionCoordinator
    ├─ 1. RedisNoncePlugin.reserveNonce()
    ├─ 2. AlchemyGasStationPlugin.estimateGas() 
    │      → Alchemy API (UserOp gas estimation)
    ├─ 3. AlchemyGasStationPlugin.applyGasStrategy()
    │      → Check Gas Policy Budget
    │      → Add Paymaster data
    ├─ 4. AlchemySmartAccountPlugin.prepareTransaction()
    │      → Build UserOperation (ERC-4337)
    │      → Include Paymaster address
    ├─ 5. AlchemySmartAccountPlugin.signTransaction()
    │      → Sign UserOperation hash
    ├─ 6. AlchemySmartAccountPlugin.sendTransaction()
    │      → Send to Alchemy Bundler
    │      → Bundler includes in bundle
    │      → EntryPoint contract processes
    │      → Paymaster pays gas
    └─ 7. RedisNoncePlugin.confirmNonce()
    ↓
UserOperationReceipt → TransactionReceipt

