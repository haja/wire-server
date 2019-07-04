{-# LANGUAGE OverloadedStrings #-}

module Gundeck.PushRelay
    (
    create
    , push
    ) where

import Data.Id (toUUID)
-- import Data.Aeson (decodeStrict)
-- import Data.Attoparsec.Text
-- import Network.HTTP.Client
import Network.HTTP.Simple
import Gundeck.Types.Push (AppName (..), Token)
import Gundeck.Push.Native.Types (Address, NativePush (..), Result (..))
import Gundeck.Monad (Gundeck)
import Imports
import System.Logger.Class ((~~), msg, val, field)

import qualified Data.UUID                 as UUID
import qualified System.Logger.Class       as Log

create :: AppName -> Token -> Gundeck ()
create _ _ = return () -- noop for now

push :: NativePush -> Address -> Gundeck Result
push (NativePush notificationId _ _) addr = do
    Log.info $ field "notificationId" (UUID.toASCIIBytes (toUUID (notificationId)))
      ~~ msg (val "pushing to push relay")
      -- TODO push: hostname:port hardcoded for now
    req' <- liftIO $ parseRequest "POST http://push-integration:8089/send"
    let req =
            setRequestBodyLBS "pushing from wire"
            $ req'
    response <- liftIO $ httpLBS req
    Log.info $ field "status" (show (getResponseStatusCode response))
      ~~ msg (val "response received")
    return $ Success addr

