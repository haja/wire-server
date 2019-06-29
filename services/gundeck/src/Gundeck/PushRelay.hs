module Gundeck.PushRelay
    (
    create
    ) where

import Imports
import Data.Aeson (decodeStrict)
import Data.Attoparsec.Text
import Gundeck.Types.Push (AppName (..), Transport (..), Token)
import Network.HTTP.Client
import Network.HTTP.Types

-- TODO remove these imports, just placeholder to fix compilation
import Gundeck.Aws (Error)
import Gundeck.Aws.Arn (EndpointArn)

create :: AppName -> Token -> Gundeck (Either Error EndpointArn)
create _ _ = return Left ()
