module Gundeck.Push.Native
    ( push
    , deleteTokens
    , module Types
    ) where

import Imports
import Control.Lens ((^.))
-- import Control.Monad.Catch
-- import Data.ByteString.Conversion.To
import Data.Id
-- import Data.List1
-- import Data.Metrics ()
-- import Gundeck.Env
import Gundeck.Monad
-- import Gundeck.Options
-- import Gundeck.Push.Native.Serialise
import Gundeck.Push.Native.Types as Types
import Gundeck.Types
import System.Logger.Class ((~~), msg, val, field)
import UnliftIO (mapConcurrently)

import qualified Data.Text                 as Text
import qualified Data.UUID                 as UUID
import qualified Gundeck.Push.Data         as Data
import qualified Gundeck.PushRelay         as PushRelay
import qualified System.Logger.Class       as Log

push :: NativePush -> [Address] -> Gundeck [Result]
push _    [] = return []
push m   [a] = pure <$> push1 m a
push m addrs = mapConcurrently (push1 m) addrs

-- TODO push: hook here, switch to pushAdapter
push1 :: NativePush -> Address -> Gundeck Result
push1 m a = do
    PushRelay.push m a

-- | Delete a list of push addresses, optionally specifying as a cause
-- a newly created address in the second argument. If such a new address
-- is given, shared owners of the deleted tokens have their addresses
-- migrated to the token and endpoint of the new address.
deleteTokens :: [Address] -> Maybe Address -> Gundeck ()
deleteTokens tokens _ = do
    forM_ tokens $ \a -> do
        Log.info $ field "user" (UUID.toASCIIBytes (toUUID (a^.addrUser)))
                ~~ field "token" (Text.take 16 (tokenText (a^.addrToken)))
                -- ~~ field "arn" (toText (a^.addrEndpoint))
                ~~ msg (val "Deleting push token")
        Data.delete (a^.addrUser) (a^.addrTransport) (a^.addrApp) (a^.addrToken)

