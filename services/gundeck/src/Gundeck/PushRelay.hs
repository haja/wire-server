{-# LANGUAGE OverloadedStrings #-}

module Gundeck.PushRelay
    (
    create
    , push
    ) where

import Control.Lens ((^.))
import Data.Id (toUUID)
import Data.Aeson (ToJSON, toJSON, object, (.=))
-- import Data.Attoparsec.Text
-- import Network.HTTP.Client
import Network.HTTP.Simple
import Gundeck.Types.Push (AppName (..), Token)
import Gundeck.Push.Native.Types (Address, NativePush (..), Result (..), addrToken, addrPushToken, addrUser)
import Gundeck.Monad (Gundeck)
import Imports
import System.Logger.Class ((~~), msg, val, field)

import qualified Data.UUID                 as UUID
import qualified System.Logger.Class       as Log

create :: AppName -> Token -> Gundeck ()
create _ _ = return () -- noop for now

push :: NativePush -> Address -> Gundeck Result
push nPush@(NativePush notificationId _ _) addr = do
    Log.info $ field "notificationId" (UUID.toASCIIBytes (toUUID (notificationId)))
            ~~ field "pushToken" (show (addr^.addrPushToken))
            ~~ msg (val "pushing to push relay")

      -- TODO push: hostname:port of push-integration hardcoded for now
    req' <- liftIO $ parseRequest "POST http://push-integration:8089/send"
    let req = setRequestBodyJSON (FcmPushRequest nPush addr)
            $ req'
    response <- liftIO $ httpLBS req

    Log.info $ field "status" (show (getResponseStatusCode response))
      ~~ msg (val "response received")
    return $ Success addr


data FcmPushRequest = FcmPushRequest !NativePush !Address

instance ToJSON FcmPushRequest where
    toJSON (FcmPushRequest _ addr) = object
        [ "validate_only" .= False
        , "message" .= object
            [
            "token" .= (addr^.addrToken)
            , "data" .= object
                [
                "user" .= (addr^.addrUser)
                ]
            ]
        ]
