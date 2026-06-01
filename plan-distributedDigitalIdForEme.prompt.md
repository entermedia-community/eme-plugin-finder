# Plan: Distributed Digital ID for EME

## Alignment & Decisions

**Architecture Decision: Peer-to-Peer Federated Identity**
- Decentralized distributed ledger for account registry
- Complements existing EME auth (OAuth2/LDAP/FileSystem remain available)
- Peer network model: independent EME instances with mutual trust
- No single control point; decentralization > centralization
- Open ecosystem; no KYC/regulatory compliance required
- REST API for mobile auth integration

**Key Requirements:**
- Mobile app can authenticate via unique worldwide Digital ID
- Accounts spread across geographically distributed instances
- Each instance maintains own copy of identity data
- Cross-instance verification without central authority

## Options Under Consideration

| Toolkit | Architecture | Blockchain? | Consensus Model | REST APIs | Self-Hosted | License |
|---------|---|---|---|---|---|---|
| **Hyperledger Indy** | DID + verifiable credentials | Yes (Indy ledger) | Stewards + voting | Yes | Yes | Apache 2.0 |
| **IPFS + DID:web** | Content-addressed DHT | No (IPFS) | Node autonomy + DNS | Yes | Yes | MIT |
| **Sprout Identity** | Lightweight distributed identity | Optional | P2P gossip | Yes | Yes | Apache 2.0 |
| **Solid (TBL)** | Decentralized personal data | No | Pod-based | Yes | Pod server | MIT |
| **Ceramic + IPFS** | Event-sourced identity graph | No (IPFS anchored) | Ceramic node sync | Yes | Yes | Apache 2.0 |

## User Selection

**Option 1: Hyperledger Indy + DIDs** ✅

## Default Assumptions (adjust as needed)
- Indy node infrastructure: Self-hosted (run own Indy nodes or join consortium)
- Wallet UX: Embedded in mobile app (Aries SDK)
- Issuer model: Distributed (each EME instance can issue credentials)
