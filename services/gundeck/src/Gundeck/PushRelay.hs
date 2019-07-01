module Gundeck.PushRelay
    (
    create
    ) where

import Imports
-- import Data.Aeson (decodeStrict)
-- import Data.Attoparsec.Text
import Gundeck.Types.Push (AppName (..), Token)
-- import Network.HTTP.Client
-- import Network.HTTP.Types

import Gundeck.Monad

-- TODO remove these imports, just placeholder to fix compilation
-- import Gundeck.Aws (Error)
-- import Gundeck.Aws.Arn (EndpointArn)

create :: AppName -> Token -> Gundeck ()
create _ _ = return () -- noop for now
